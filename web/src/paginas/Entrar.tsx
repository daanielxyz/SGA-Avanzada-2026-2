import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { iniciarSesion, solicitarPin } from '../api/cuenta'
import { useSesion } from '../sesion/useSesion'

// DEC-68: el PIN vale 15 minutos y sirve una sola vez; si esa regla cambia en el backend, cambia aquí
const MINUTOS_PIN = 15
const SIN_CONEXION = 'No pudimos comunicarnos con el servidor. Intenta de nuevo en un momento.'
const mensajeDe = (error: unknown) => (error instanceof ErrorApi ? error.message : SIN_CONEXION)

// Solo una página de esta misma web: un enlace armado no puede mandar al huésped a otro sitio después de entrar
function destinoSeguro(volver: string | null) {
  return volver?.startsWith('/') && !volver.startsWith('//') && !volver.includes('\\') ? volver : '/mis-reservas'
}

/**
 * Entrar sin contraseña (DEC-68): correo y documento, y luego el PIN de 6 dígitos que llega al correo. El mensaje al
 * pedirlo es el mismo exista o no la cuenta, así que el error solo puede aparecer al escribir el PIN.
 */
export default function Entrar() {
  const { sesion, perdida, entrar } = useSesion()
  const navegar = useNavigate()
  const [consulta] = useSearchParams()
  const destino = destinoSeguro(consulta.get('volver'))

  const [correo, setCorreo] = useState('')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [pin, setPin] = useState('')
  const [problema, setProblema] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [pidiendoPin, setPidiendoPin] = useState(false)
  const campoPin = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (pidiendoPin) campoPin.current?.focus()
  }, [pidiendoPin])

  if (sesion) {
    return <Navigate to={destino} replace />
  }

  async function pedirPin(evento?: FormEvent) {
    evento?.preventDefault()
    setProblema(null)
    setEnviando(true)
    try {
      await solicitarPin({ correo: correo.trim(), numeroDocumento: numeroDocumento.trim() })
      setPin('')
      setPidiendoPin(true)
      campoPin.current?.focus()
    } catch (error) {
      setProblema(mensajeDe(error))
    } finally {
      setEnviando(false)
    }
  }

  async function confirmarPin(evento: FormEvent) {
    evento.preventDefault()
    setProblema(null)
    setEnviando(true)
    try {
      const { token, expiraEn, nombre } = await iniciarSesion(
        { correo: correo.trim(), numeroDocumento: numeroDocumento.trim() }, pin)
      entrar({ token, expiraEn, nombre })
      navegar(destino, { replace: true })
    } catch (error) {
      setProblema(mensajeDe(error))
      setPin('')
      setEnviando(false)
      campoPin.current?.focus()
    }
  }

  return (
    <main className="ficha entrar">
      <h1>{pidiendoPin ? 'Revisa tu correo' : 'Entra a tus reservas'}</h1>
      {perdida && !pidiendoPin && <p role="status">Tu sesión terminó. Vuelve a entrar para continuar.</p>}

      {!pidiendoPin && (
        <form className="tarjeta-formulario" onSubmit={pedirPin}>
          <p className="suave">Usa el correo y el documento con los que reservaste. No necesitas contraseña.</p>
          <label>
            Correo
            <input type="email" value={correo} maxLength={150} required autoComplete="email"
              onChange={(e) => setCorreo(e.target.value)} />
          </label>
          <label>
            Número de documento
            <input value={numeroDocumento} maxLength={30} required autoComplete="off"
              onChange={(e) => setNumeroDocumento(e.target.value)} />
          </label>
          {problema && <p role="alert" className="aviso">{problema}</p>}
          <button type="submit" className="accion-principal" disabled={enviando}>
            {enviando ? 'Enviando el PIN…' : 'Enviarme un PIN'}
          </button>
        </form>
      )}

      {pidiendoPin && (
        <form className="tarjeta-formulario" onSubmit={confirmarPin}>
          <p role="status">
            Si los datos corresponden a una cuenta, te enviamos un PIN a <strong>{correo.trim()}</strong>. Vale
            por {MINUTOS_PIN} minutos y una sola vez; si no lo ves, revisa el correo no deseado.
          </p>
          <label>
            PIN de 6 dígitos
            <input ref={campoPin} className="campo-pin" value={pin} inputMode="numeric" autoComplete="one-time-code"
              required pattern="\d{6}" maxLength={6} title="Escribe los 6 dígitos del PIN"
              onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} />
          </label>
          {problema && <p role="alert" className="aviso">{problema}</p>}
          <button type="submit" className="accion-principal" disabled={enviando}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
          <div className="acciones-pin">
            <button type="button" className="secundario" disabled={enviando} onClick={() => pedirPin()}>
              Enviar otro PIN
            </button>
            <button type="button" className="secundario" disabled={enviando} onClick={() => {
              setPidiendoPin(false)
              setProblema(null)
            }}>
              Cambiar correo o documento
            </button>
          </div>
        </form>
      )}
    </main>
  )
}
