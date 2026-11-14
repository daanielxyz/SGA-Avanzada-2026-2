import { useRef, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ErrorApi } from '../api/cliente'
import Aviso from '../componentes/Aviso'
import Logo from '../componentes/Logo'
import { useTitulo } from '../componentes/useTitulo'
import { useSesion } from '../sesion/contexto'

/** Lo que salió mal al entrar, tal como lo explica el backend. */
interface Problema {
  mensaje: string
  detalles: string[]
}

const AVISO_SESION_PERDIDA = 'Tu sesión terminó. Vuelve a entrar.'

/** Entrada del personal con su correo y contraseña (CU-02). */
export default function IniciarSesion() {
  const { sesion, motivo, aviso: avisoDeSesion, entrar } = useSesion()
  const navegar = useNavigate()
  const estado = useLocation().state as { desde?: string; aviso?: string } | null
  const desde = estado?.desde ?? '/'
  // Al volver de activar la cuenta o de cambiar la contraseña, se dice que salió bien
  const exito = estado?.aviso ?? (motivo === 'cuenta-cambiada' ? avisoDeSesion : null)
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [verContrasena, setVerContrasena] = useState(false)
  const [faltaCorreo, setFaltaCorreo] = useState(false)
  const [faltaContrasena, setFaltaContrasena] = useState(false)
  const [entrando, setEntrando] = useState(false)
  const [problema, setProblema] = useState<Problema | null>(null)
  const campoCorreo = useRef<HTMLInputElement>(null)
  const campoContrasena = useRef<HTMLInputElement>(null)
  useTitulo('Iniciar sesión')

  if (sesion && !entrando) {
    return <Navigate to={desde} replace />
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    const sinCorreo = !correo.trim()
    const sinContrasena = !contrasena
    setFaltaCorreo(sinCorreo)
    setFaltaContrasena(sinContrasena)
    setProblema(null)
    if (sinCorreo || sinContrasena) {
      ;(sinCorreo ? campoCorreo : campoContrasena).current?.focus()
      return
    }
    setEntrando(true)
    try {
      await entrar(correo.trim(), contrasena)
      navegar(desde, { replace: true })
    } catch (error) {
      setProblema(error instanceof ErrorApi
        ? { mensaje: error.message, detalles: error.detalles }
        : { mensaje: 'Algo salió mal. Intenta de nuevo.', detalles: [] })
      setEntrando(false)
    }
  }

  return (
    <main className="entrada">
      <div className="entrada-arte">
        <Logo decorativo />
        <div className="entrada-lema">
          <p className="entrada-lema-titulo">Cada detalle bajo control.</p>
          <p className="entrada-lema-texto">La operación en su punto más alto.</p>
        </div>
      </div>

      <div className="entrada-formulario">
        <form className="formulario" onSubmit={enviar} noValidate aria-busy={entrando}>
          <Logo apilado />
          <div className="entrada-titulo">
            <h1>Te damos la bienvenida</h1>
            <p className="suave">Todo listo para que tu operación fluya.</p>
          </div>

          {exito && !problema && <Aviso tipo="exito" mensaje={exito} />}
          {motivo === 'perdida' && !exito && !problema && <Aviso tipo="info" mensaje={AVISO_SESION_PERDIDA} />}
          {problema && <Aviso tipo="error" mensaje={problema.mensaje} detalles={problema.detalles} />}

          <div>
            <label className="campo-rotulo" htmlFor="correo">
              Correo electrónico
            </label>
            <input
              ref={campoCorreo}
              id="correo"
              className="campo-control campo-control-grande"
              type="email"
              placeholder="tu@correo.com"
              autoComplete="username"
              value={correo}
              disabled={entrando}
              aria-invalid={faltaCorreo}
              aria-describedby={faltaCorreo ? 'correo-falta' : undefined}
              onChange={(e) => {
                setCorreo(e.target.value)
                setFaltaCorreo(false)
              }}
            />
            {faltaCorreo && (
              <p id="correo-falta" className="campo-mensaje">
                Escribe tu correo electrónico.
              </p>
            )}
          </div>

          <div>
            <label className="campo-rotulo" htmlFor="contrasena">
              Contraseña
            </label>
            <div className="campo-clave">
              <input
                ref={campoContrasena}
                id="contrasena"
                className="campo-control campo-control-grande"
                type={verContrasena ? 'text' : 'password'}
                placeholder="Ingresa tu contraseña"
                autoComplete="current-password"
                value={contrasena}
                disabled={entrando}
                aria-invalid={faltaContrasena}
                aria-describedby={faltaContrasena ? 'contrasena-falta' : undefined}
                onChange={(e) => {
                  setContrasena(e.target.value)
                  setFaltaContrasena(false)
                }}
              />
              <button
                type="button"
                className="ver-clave"
                aria-label={verContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                disabled={entrando}
                onClick={() => setVerContrasena(!verContrasena)}
              >
                {verContrasena ? 'Ocultar' : 'Mostrar'}
              </button>
            </div>
            {faltaContrasena && (
              <p id="contrasena-falta" className="campo-mensaje">
                Escribe tu contraseña.
              </p>
            )}
          </div>

          <button type="submit" className="boton boton-bloque boton-grande" disabled={entrando}>
            {entrando ? 'Entrando…' : 'Entrar'}
          </button>

          <p className="entrada-enlaces">
            <Link to="/recuperar" state={{ correo: correo.trim() }} className="enlace">Olvidé mi contraseña</Link>
            <Link to="/activar" className="enlace">Activar mi cuenta</Link>
          </p>
        </form>
      </div>
    </main>
  )
}
