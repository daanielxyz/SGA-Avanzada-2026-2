import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { ErrorApi } from '../api/cliente'
import {
  actualizarCuenta,
  confirmarCambioCorreo,
  consultarCuenta,
  solicitarCambioCorreo,
  type Cuenta,
} from '../api/cuenta'
import type { TipoDocumento } from '../api/reservas'
import CampoTelefono from '../reservar/CampoTelefono'

const DOCUMENTOS: Record<TipoDocumento, string> = {
  CC: 'Cédula de ciudadanía',
  CE: 'Cédula de extranjería',
  PASAPORTE: 'Pasaporte',
  TI: 'Tarjeta de identidad',
}

// DEC-69: el PIN del cambio de correo vale 15 minutos; si esa regla cambia en el backend, cambia aquí
const MINUTOS_PIN = 15
const mensajeDe = (error: unknown, siNoResponde: string) => (error instanceof ErrorApi ? error.message : siNoResponde)
const opcional = (texto: string) => texto.trim() || undefined

type Modo = 'ver' | 'editar' | 'correo'

/**
 * Los datos del huésped (DEC-68): cambia su teléfono y, mientras el hotel no haya validado su identidad, su nombre
 * (DEC-98). El correo cambia en dos pasos, con un PIN que llega al correo actual y al teléfono (DEC-69). El documento
 * nunca cambia: lo corrige el hotel.
 */
export default function MiCuenta() {
  const [cuenta, setCuenta] = useState<Cuenta | null>(null)
  const [problema, setProblema] = useState<string | null>(null)
  const [modo, setModo] = useState<Modo>('ver')
  const [aviso, setAviso] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    consultarCuenta()
      .then((c) => vigente && setCuenta(c))
      .catch((error: unknown) => vigente && setProblema(
        mensajeDe(error, 'No pudimos abrir tu cuenta. Intenta de nuevo en un momento.')))
    return () => {
      vigente = false
    }
  }, [])

  function terminar(nueva: Cuenta, mensaje: string) {
    setCuenta(nueva)
    setModo('ver')
    setAviso(mensaje)
  }

  function cambiarModo(nuevo: Modo) {
    setAviso(null)
    setModo(nuevo)
  }

  return (
    <main className="ficha cuenta">
      <h1>Mi cuenta</h1>
      {problema && <p role="alert" className="aviso">{problema}</p>}
      {!cuenta && !problema && <p className="suave">Cargando tus datos…</p>}
      {cuenta && modo === 'ver' && (
        <>
          {aviso && <p role="status" className="aviso-cambio">{aviso}</p>}
          <dl className="datos">
            <dt>Nombre</dt>
            <dd>{cuenta.nombre}</dd>
            <dt>Documento</dt>
            <dd>{DOCUMENTOS[cuenta.tipoDocumento]} {cuenta.numeroDocumento}</dd>
            <dt>Correo</dt>
            <dd>{cuenta.correo ?? 'Sin correo'}</dd>
            <dt>Teléfono</dt>
            <dd>{cuenta.telefono ?? 'Sin teléfono'}</dd>
          </dl>
          {cuenta.validado && (
            <p className="suave">El hotel ya validó tu identidad. Para corregir tu nombre o tu documento, pídelo en
              recepción.</p>
          )}
          <div className="acciones-cuenta">
            <button type="button" className="accion-principal" onClick={() => cambiarModo('editar')}>
              {cuenta.validado ? 'Cambiar mi teléfono' : 'Editar mis datos'}
            </button>
            {cuenta.correo && (
              <button type="button" className="secundario" onClick={() => cambiarModo('correo')}>
                Cambiar mi correo
              </button>
            )}
          </div>
        </>
      )}
      {cuenta && modo === 'editar' && (
        <FormularioDatos cuenta={cuenta} alCancelar={() => cambiarModo('ver')}
          alGuardar={(nueva) => terminar(nueva, 'Tus datos quedaron guardados.')} />
      )}
      {cuenta && modo === 'correo' && (
        <CambioCorreo cuenta={cuenta} alCancelar={() => cambiarModo('ver')}
          alCambiar={(nueva) => terminar(nueva, `Tu correo quedó cambiado. La próxima vez entra con ${
            nueva.correo ?? 'el correo nuevo'}.`)} />
      )}
      <Link to="/mis-reservas">Ver mis reservas</Link>
    </main>
  )
}

interface PropsDatos {
  cuenta: Cuenta
  alGuardar: (cuenta: Cuenta) => void
  alCancelar: () => void
}

function FormularioDatos({ cuenta, alGuardar, alCancelar }: PropsDatos) {
  const [nombres, setNombres] = useState(cuenta.nombres)
  // Los apellidos se editan juntos en un solo campo, como al reservar
  const [apellidos, setApellidos] = useState(
    [cuenta.primerApellido, cuenta.segundoApellido].filter(Boolean).join(' '))
  const [telefono, setTelefono] = useState(cuenta.telefono ?? '')
  const [enviando, setEnviando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setProblema(null)
    setEnviando(true)
    try {
      // Con la identidad validada se envía el nombre que ya tiene: el backend solo deja cambiar el teléfono
      alGuardar(await actualizarCuenta(cuenta.validado
        ? { nombres: cuenta.nombres, primerApellido: cuenta.primerApellido ?? undefined,
          segundoApellido: cuenta.segundoApellido ?? undefined, telefono }
        : { nombres: nombres.trim(), primerApellido: opcional(apellidos), telefono }))
    } catch (error) {
      setProblema(mensajeDe(error, 'No pudimos guardar tus datos. Intenta de nuevo en un momento.'))
      setEnviando(false)
    }
  }

  return (
    <form className="tarjeta-formulario" onSubmit={guardar}>
      {cuenta.validado ? (
        <p className="suave">
          Tu nombre, <strong>{cuenta.nombre}</strong>, ya lo validó el hotel: si está mal, pídelo en recepción.
        </p>
      ) : (
        <>
          <label>
            Nombres
            <input value={nombres} maxLength={120} pattern=".*\S.*" required autoComplete="given-name"
              onChange={(e) => setNombres(e.target.value)} />
          </label>
          <label>
            Apellidos
            <input value={apellidos} maxLength={60} autoComplete="family-name"
              onChange={(e) => setApellidos(e.target.value)} />
          </label>
        </>
      )}
      <CampoTelefono inicial={cuenta.telefono} alCambiar={setTelefono} />
      {problema && <p role="alert" className="aviso">{problema}</p>}
      <button type="submit" className="accion-principal" disabled={enviando}>
        {enviando ? 'Guardando…' : 'Guardar'}
      </button>
      <button type="button" className="secundario" disabled={enviando} onClick={alCancelar}>Cancelar</button>
    </form>
  )
}

interface PropsCorreo {
  cuenta: Cuenta
  alCambiar: (cuenta: Cuenta) => void
  alCancelar: () => void
}

function CambioCorreo({ cuenta, alCambiar, alCancelar }: PropsCorreo) {
  const [nuevoCorreo, setNuevoCorreo] = useState('')
  const [pin, setPin] = useState('')
  const [pidiendoPin, setPidiendoPin] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)
  const campoPin = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (pidiendoPin) campoPin.current?.focus()
  }, [pidiendoPin])

  async function pedirPin(evento?: FormEvent) {
    evento?.preventDefault()
    setProblema(null)
    setEnviando(true)
    try {
      await solicitarCambioCorreo(cuenta, nuevoCorreo.trim())
      setPin('')
      setPidiendoPin(true)
    } catch (error) {
      setProblema(mensajeDe(error, 'No pudimos enviar el PIN. Intenta de nuevo en un momento.'))
    } finally {
      setEnviando(false)
    }
  }

  async function confirmar(evento: FormEvent) {
    evento.preventDefault()
    setProblema(null)
    setEnviando(true)
    try {
      await confirmarCambioCorreo(cuenta, pin)
      alCambiar(await consultarCuenta())
    } catch (error) {
      setProblema(mensajeDe(error, 'No pudimos cambiar tu correo. Intenta de nuevo en un momento.'))
      setPin('')
      setEnviando(false)
      campoPin.current?.focus()
    }
  }

  if (!pidiendoPin) {
    return (
      <form className="tarjeta-formulario" onSubmit={pedirPin}>
        <p className="suave">
          Para que nadie más cambie tu correo, te enviamos un PIN a tu correo actual, <strong>{cuenta.correo}</strong>,
          y a tu teléfono.
        </p>
        <label>
          Correo nuevo
          <input type="email" value={nuevoCorreo} maxLength={150} required autoComplete="email"
            onChange={(e) => setNuevoCorreo(e.target.value)} />
        </label>
        {problema && <p role="alert" className="aviso">{problema}</p>}
        <button type="submit" className="accion-principal" disabled={enviando}>
          {enviando ? 'Enviando el PIN…' : 'Enviarme un PIN'}
        </button>
        <button type="button" className="secundario" disabled={enviando} onClick={alCancelar}>Cancelar</button>
      </form>
    )
  }

  return (
    <form className="tarjeta-formulario" onSubmit={confirmar}>
      <p role="status">
        Te enviamos un PIN a <strong>{cuenta.correo}</strong> y a tu teléfono para cambiar el correo
        a <strong>{nuevoCorreo.trim()}</strong>. Vale por {MINUTOS_PIN} minutos y una sola vez; si no lo ves, revisa
        el correo no deseado.
      </p>
      <label>
        PIN de 6 dígitos
        <input ref={campoPin} className="campo-pin" value={pin} inputMode="numeric" autoComplete="one-time-code"
          required pattern="\d{6}" maxLength={6} title="Escribe los 6 dígitos del PIN"
          onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} />
      </label>
      {problema && <p role="alert" className="aviso">{problema}</p>}
      <button type="submit" className="accion-principal" disabled={enviando}>
        {enviando ? 'Cambiando…' : 'Cambiar mi correo'}
      </button>
      <div className="acciones-pin">
        <button type="button" className="secundario" disabled={enviando} onClick={() => pedirPin()}>
          Enviar otro PIN
        </button>
        <button type="button" className="secundario" disabled={enviando} onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
