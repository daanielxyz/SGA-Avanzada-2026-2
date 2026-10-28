import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { useSesion } from '../sesion/contexto'

/** Lo que salió mal al entrar, tal como lo explica el backend. */
interface Problema {
  mensaje: string
  detalles: string[]
}

const AVISO_SESION_PERDIDA = 'Tu sesión terminó. Vuelve a entrar para continuar.'

/** Entrada del personal con su correo y contraseña (CU-02). */
export default function IniciarSesion() {
  const { sesion, motivo, entrar } = useSesion()
  const navegar = useNavigate()
  const desde = (useLocation().state as { desde?: string } | null)?.desde ?? '/'
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [entrando, setEntrando] = useState(false)
  const [problema, setProblema] = useState<Problema | null>(null)

  if (sesion && !entrando) {
    return <Navigate to={desde} replace />
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!correo.trim() || !contrasena) {
      setProblema({ mensaje: 'Escribe tu correo y tu contraseña.', detalles: [] })
      return
    }
    setEntrando(true)
    setProblema(null)
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
      <h1>Gestión del alojamiento</h1>
      <p className="suave">Entra con la cuenta que te dio el administrador.</p>
      {motivo === 'perdida' && !problema && <p className="nota">{AVISO_SESION_PERDIDA}</p>}

      <form className="formulario" onSubmit={enviar} noValidate>
        <label>
          Correo
          <input type="email" autoComplete="username" value={correo} onChange={(e) => setCorreo(e.target.value)} />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            autoComplete="current-password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
          />
        </label>
        <button type="submit" disabled={entrando}>
          {entrando ? 'Entrando…' : 'Entrar'}
        </button>
        {problema && (
          <div role="alert" className="aviso">
            <p>{problema.mensaje}</p>
            {problema.detalles.length > 0 && (
              <ul>
                {problema.detalles.map((detalle) => (
                  <li key={detalle}>{detalle}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </form>
    </main>
  )
}
