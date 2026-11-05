import { useEffect, useState, type FormEvent } from 'react'
import type { Alojamiento } from '../api/alojamiento'
import type { Estadia, Grupo, Precio } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { cotizar, reservar, type ReservaApartada, type TipoDocumento } from '../api/reservas'
import { enPesos, fechaDesdeHoy } from '../formato'

interface Props {
  codigo: string
  estadia: Estadia
  grupo: Grupo
  alojamiento: Alojamiento | null
  /** Lo que se le mostró al buscar, con los adultos y niños que dijo. */
  precioBuscado: Precio
  alCambiarPrecio: (precio: Precio) => void
  alReservar: (apartada: ReservaApartada) => void
}

interface Persona {
  nombre: string
  fechaNacimiento: string
}

const TIPOS_DOCUMENTO: { valor: TipoDocumento; texto: string }[] = [
  { valor: 'CC', texto: 'Cédula de ciudadanía' },
  { valor: 'CE', texto: 'Cédula de extranjería' },
  { valor: 'PASAPORTE', texto: 'Pasaporte' },
  { valor: 'TI', texto: 'Tarjeta de identidad' },
]

// El navegador envía la hora como HH:mm; el backend la da como HH:mm:ss
const aHoraDeCampo = (hora: string | undefined) => hora?.slice(0, 5) ?? ''

/**
 * Paso 1: quién viaja. El titular es la primera persona del grupo (TIT-01). Con todas las fechas de nacimiento, el
 * total se recalcula con la regla real (RN-06): si no coincide con lo que dijo al buscar, se le avisa antes de pagar.
 */
export default function PasoDatos({ codigo, estadia, grupo, alojamiento, precioBuscado, alCambiarPrecio,
  alReservar }: Props) {
  const hoy = fechaDesdeHoy()
  const [personas, setPersonas] = useState<Persona[]>(() =>
    Array.from({ length: grupo.adultos + grupo.ninos }, () => ({ nombre: '', fechaNacimiento: '' })))
  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>('CC')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [correo, setCorreo] = useState('')
  const [telefono, setTelefono] = useState('')
  const [horaLlegada, setHoraLlegada] = useState('')
  const [vehiculos, setVehiculos] = useState(0)
  const [mascotas, setMascotas] = useState(0)
  const [precioReal, setPrecioReal] = useState<Precio | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [problema, setProblema] = useState<{ mensaje: string; detalles: string[] } | null>(null)

  const parametros = alojamiento?.parametros
  const cargoMascota = parametros?.cargoMascota ?? 0
  const { entrada, salida } = estadia
  const hora = horaLlegada || aHoraDeCampo(parametros?.horaEntrada)
  const fechas = personas.map((p) => p.fechaNacimiento)
  const fechasListas = fechas.every((f) => f !== '' && f <= hoy)
  const claveFechas = fechasListas ? fechas.join('|') : null

  useEffect(() => {
    if (claveFechas === null) return
    let vigente = true // si cambian las fechas antes de que responda, se ignora la respuesta vieja
    cotizar(codigo, { entrada, salida }, claveFechas.split('|'))
      .then((precio) => {
        if (vigente) {
          setPrecioReal(precio)
          alCambiarPrecio(precio)
        }
      })
      // El total no bloquea: si esta consulta falla, el backend lo vuelve a calcular al reservar
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [codigo, entrada, salida, claveFechas, alCambiarPrecio])

  function cambiarPersona(indice: number, cambio: Partial<Persona>) {
    setPersonas((actuales) => actuales.map((p, i) => (i === indice ? { ...p, ...cambio } : p)))
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setProblema(null)
    setEnviando(true)
    const [titular, ...acompanantes] = personas
    try {
      alReservar(await reservar({
        apartamento: codigo,
        ...estadia,
        titular: { nombre: titular.nombre, tipoDocumento, numeroDocumento, correo, telefono },
        ocupantes: [{ ...titular, tipoDocumento, numeroDocumento }, ...acompanantes],
        horaEstimadaLlegada: hora,
        vehiculos,
        mascotas,
      }))
    } catch (error) {
      setProblema(error instanceof ErrorApi
        ? { mensaje: error.message, detalles: error.detalles }
        : { mensaje: 'No pudimos crear tu reserva. Intenta de nuevo.', detalles: [] })
      setEnviando(false)
    }
  }

  const cambio = precioReal && precioReal.total !== precioBuscado.total ? precioReal : null
  const pagan = cambio?.desglose[0]?.ocupantesFacturables
  const hayExtras = (parametros?.cupoParqueadero ?? 0) > 0 || parametros?.aceptaMascotas

  return (
    <form className="formulario-reserva" onSubmit={enviar}>
      <fieldset>
        <legend>Quien reserva</legend>
        <p className="suave">A este correo te llega la confirmación, y con él entras a ver tu reserva.</p>
        <div className="campos">
          <CampoPersona indice={0} persona={personas[0]} hoy={hoy} alCambiar={cambiarPersona} />
          <label>
            Tipo de documento
            <select value={tipoDocumento} onChange={(e) => setTipoDocumento(e.target.value as TipoDocumento)}>
              {TIPOS_DOCUMENTO.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
            </select>
          </label>
          <label>
            Número de documento
            <input value={numeroDocumento} maxLength={30} required autoComplete="off"
              onChange={(e) => setNumeroDocumento(e.target.value)} />
          </label>
          <label>
            Correo
            <input type="email" value={correo} maxLength={150} required autoComplete="email"
              onChange={(e) => setCorreo(e.target.value)} />
          </label>
          <label>
            Teléfono
            <input type="tel" value={telefono} maxLength={30} required autoComplete="tel"
              onChange={(e) => setTelefono(e.target.value)} />
          </label>
        </div>
      </fieldset>

      {personas.length > 1 && (
        <fieldset>
          <legend>Quienes viajan contigo</legend>
          {personas.slice(1).map((persona, i) => (
            <div className="campos" key={i} role="group" aria-label={`Persona ${i + 2}`}>
              <CampoPersona indice={i + 1} persona={persona} hoy={hoy} alCambiar={cambiarPersona} />
            </div>
          ))}
        </fieldset>
      )}

      <fieldset>
        <legend>Tu llegada</legend>
        <div className="campos">
          <label>
            Hora estimada de llegada
            <input type="time" value={hora} required onChange={(e) => setHoraLlegada(e.target.value)} />
          </label>
        </div>
        {hayExtras && (
          <details className="extras">
            <summary>¿Traes carro o mascota?</summary>
            <div className="campos">
              {(parametros?.cupoParqueadero ?? 0) > 0 && (
                <label>
                  Carros
                  <input type="number" inputMode="numeric" min={0} value={vehiculos}
                    onChange={(e) => setVehiculos(Math.max(0, Number(e.target.value) || 0))} />
                </label>
              )}
              {parametros?.aceptaMascotas && (
                <div className="campo">
                  <label htmlFor="mascotas">Mascotas</label>
                  <input id="mascotas" type="number" inputMode="numeric" min={0} max={parametros.limiteMascotas}
                    value={mascotas} aria-describedby="mascotas-ayuda"
                    onChange={(e) => setMascotas(Math.max(0, Number(e.target.value) || 0))} />
                  <span id="mascotas-ayuda" className="ayuda">
                    Hasta {parametros.limiteMascotas}
                    {cargoMascota > 0 && `; cada una suma ${enPesos(cargoMascota)} a tu cuenta`}
                  </span>
                </div>
              )}
            </div>
          </details>
        )}
      </fieldset>

      {cambio && (
        <p className="aviso-cambio" role="status">
          Tu total cambió a <strong>{enPesos(cambio.total)}</strong>: según las fechas de nacimiento,
          pagan {pagan}{' '}
          {pagan === 1 ? 'persona' : 'personas'}. La edad que cuenta es la del día de llegada.
        </p>
      )}
      {problema && (
        <div role="alert" className="aviso">
          <p>{problema.mensaje}</p>
          {problema.detalles.length > 0 && <ul>{problema.detalles.map((d) => <li key={d}>{d}</li>)}</ul>}
        </div>
      )}
      <button type="submit" className="accion-principal" disabled={enviando}>
        {enviando ? 'Apartando tu reserva…' : 'Continuar al pago'}
      </button>
    </form>
  )
}

interface PropsPersona {
  indice: number
  persona: Persona
  hoy: string
  alCambiar: (indice: number, cambio: Partial<Persona>) => void
}

function CampoPersona({ indice, persona, hoy, alCambiar }: PropsPersona) {
  const titular = indice === 0
  return (
    <>
      <label>
        {titular ? 'Nombre completo' : `Nombre de la persona ${indice + 1}`}
        <input value={persona.nombre} maxLength={120} required autoComplete={titular ? 'name' : 'off'}
          onChange={(e) => alCambiar(indice, { nombre: e.target.value })} />
      </label>
      <label>
        {titular ? 'Fecha de nacimiento' : `Fecha de nacimiento de la persona ${indice + 1}`}
        <input type="date" value={persona.fechaNacimiento} max={hoy} required
          onChange={(e) => alCambiar(indice, { fechaNacimiento: e.target.value })} />
      </label>
    </>
  )
}
