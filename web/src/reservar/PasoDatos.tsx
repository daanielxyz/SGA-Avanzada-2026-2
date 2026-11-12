import { useEffect, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import type { Alojamiento } from '../api/alojamiento'
import type { Estadia, Grupo, Precio } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { consultarCuenta, type Cuenta } from '../api/cuenta'
import { cotizar, reservar, type Extras, type ReservaApartada, type TipoDocumento } from '../api/reservas'
import { enPesos, fechaDesdeHoy, horaDelDia, noches } from '../formato'
import { leerSesion } from '../sesion/almacen'
import CampoTelefono from './CampoTelefono'

interface Props {
  codigo: string
  estadia: Estadia
  grupo: Grupo
  alojamiento: Alojamiento | null
  /** Las que admite este apartamento: el hotel decide si las acepta y el apartamento puede tener su límite (DEC-93). */
  limiteMascotas: number
  /** Lo que se le mostró al buscar, con los adultos y niños que dijo. */
  precioBuscado: Precio
  alCambiarPrecio: (precio: Precio) => void
  /** Lo que suman el parqueadero y las mascotas al total. */
  alCambiarExtras: (extras: Extras) => void
  alReservar: (apartada: ReservaApartada) => void
}

interface Persona {
  nombres: string
  apellidos: string
  fechaNacimiento: string
}

const TIPOS_DOCUMENTO: { valor: TipoDocumento; texto: string }[] = [
  { valor: 'CC', texto: 'Cédula de ciudadanía' },
  { valor: 'CE', texto: 'Cédula de extranjería' },
  { valor: 'PASAPORTE', texto: 'Pasaporte' },
  { valor: 'TI', texto: 'Tarjeta de identidad' },
]
const TEXTO_DOCUMENTO = Object.fromEntries(TIPOS_DOCUMENTO.map((t) => [t.valor, t.texto])) as Record<TipoDocumento,
  string>

// Dos campos: nombres y apellidos. Los apellidos van juntos, como los escribe, en primerApellido (BL-07 · DEC-98)
const nombreDe = ({ nombres, apellidos }: Pick<Persona, 'nombres' | 'apellidos'>) =>
  ({ nombres: nombres.trim(), primerApellido: apellidos.trim() })

// Con sesión, el nombre sale de la cuenta tal como está guardado; las cuentas viejas no traen apellidos (DEC-98)
const nombreDeCuenta = ({ nombres, primerApellido, segundoApellido }: Cuenta) => ({
  nombres,
  ...(primerApellido ? { primerApellido } : {}),
  ...(segundoApellido ? { segundoApellido } : {}),
})

/**
 * Paso 1: quién reserva y quiénes se hospedan. Quien reserva responde por la reserva y su pago, y puede no hospedarse
 * (DEC-97): la casilla «Yo también me hospedo» decide si es la primera persona del grupo. El backend toma como huésped
 * principal al primer mayor de edad. Con sesión, quien reserva es la cuenta: sus datos se muestran y se cambian en «Mi
 * cuenta», porque el backend usa los guardados (DEC-69). Con todas las fechas de nacimiento, el total se recalcula con
 * la regla real (RN-06): si no coincide con lo que dijo al buscar, se le avisa antes de pagar.
 */
export default function PasoDatos({ codigo, estadia, grupo, alojamiento, limiteMascotas, precioBuscado,
  alCambiarPrecio, alCambiarExtras, alReservar }: Props) {
  const hoy = fechaDesdeHoy()
  const [conSesion] = useState(() => leerSesion() !== null)
  const [cuenta, setCuenta] = useState<Cuenta | null>(null)
  const [cargandoCuenta, setCargandoCuenta] = useState(conSesion)
  const [meHospedo, setMeHospedo] = useState(true)
  const [quien, setQuien] = useState({ nombres: '', apellidos: '' })
  // Una por cada persona del grupo; si quien reserva se hospeda, la primera es él y solo se usa su fecha
  const [personas, setPersonas] = useState<Persona[]>(() =>
    Array.from({ length: grupo.adultos + grupo.ninos },
      () => ({ nombres: '', apellidos: '', fechaNacimiento: '' })))
  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>('CC')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [correo, setCorreo] = useState('')
  const [telefono, setTelefono] = useState('')
  const [carros, setCarros] = useState(0)
  const [motos, setMotos] = useState(0)
  const [mascotas, setMascotas] = useState(0)
  const [precioReal, setPrecioReal] = useState<Precio | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [problema, setProblema] = useState<{ mensaje: string; detalles: string[] } | null>(null)

  const parametros = alojamiento?.parametros
  const cargoMascota = parametros?.cargoMascota ?? 0
  const { entrada, salida } = estadia
  const cargoCarro = parametros?.cargoCarro ?? 0
  const cargoMoto = parametros?.cargoMoto ?? 0
  const porNoche = parametros?.cobroParqueadero !== 'POR_ESTADIA'
  // Solo para mostrarlo mientras escribe: lo que se cobra lo calcula el backend al crear la reserva (DEC-89)
  const porParqueadero = (carros * cargoCarro + motos * cargoMoto) * (porNoche ? noches(entrada, salida) : 1)
  const porMascotas = Math.min(mascotas, limiteMascotas) * cargoMascota
  const totalExtras = porParqueadero + porMascotas
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

  useEffect(() => {
    alCambiarExtras({ parqueadero: porParqueadero, mascotas: porMascotas, total: porParqueadero + porMascotas })
  }, [porParqueadero, porMascotas, alCambiarExtras])

  useEffect(() => {
    if (!conSesion) return
    let vigente = true
    consultarCuenta()
      .then((c) => vigente && setCuenta(c))
      // Si la cuenta no carga, se piden los datos como sin sesión: nada impide reservar
      .catch(() => {})
      .finally(() => vigente && setCargandoCuenta(false))
    return () => {
      vigente = false
    }
  }, [conSesion])

  function cambiarPersona(indice: number, cambio: Partial<Persona>) {
    setPersonas((actuales) => actuales.map((p, i) => (i === indice ? { ...p, ...cambio } : p)))
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setProblema(null)
    setEnviando(true)
    const nombre = cuenta ? nombreDeCuenta(cuenta) : nombreDe(quien)
    const documento = cuenta
      ? { tipoDocumento: cuenta.tipoDocumento, numeroDocumento: cuenta.numeroDocumento }
      : { tipoDocumento, numeroDocumento }
    const [primera, ...resto] = personas
    const huespedes = (lista: Persona[]) => lista.map((p) => ({ ...nombreDe(p), fechaNacimiento: p.fechaNacimiento }))
    try {
      alReservar(await reservar({
        apartamento: codigo,
        ...estadia,
        titular: { ...nombre, ...documento, correo: cuenta?.correo ?? correo, telefono: cuenta?.telefono ?? telefono },
        ocupantes: meHospedo
          ? [{ ...nombre, fechaNacimiento: primera.fechaNacimiento, ...documento }, ...huespedes(resto)]
          : huespedes(personas),
        carros,
        motos,
        mascotas,
      }))
      setEnviando(false) // al volver desde el pago, el formulario se puede enviar otra vez
    } catch (error) {
      setProblema(error instanceof ErrorApi
        ? { mensaje: error.message, detalles: error.detalles }
        : { mensaje: 'No pudimos crear tu reserva. Intenta de nuevo.', detalles: [] })
      setEnviando(false)
    }
  }

  const cambio = precioReal && precioReal.total !== precioBuscado.total ? precioReal : null
  const pagan = cambio?.desglose[0]?.ocupantesFacturables
  const hayExtras = (parametros?.cupoParqueadero ?? 0) > 0 || limiteMascotas > 0
  const horaEntrada = parametros?.horaEntrada
  const horaSalida = parametros?.horaSalida
  const hayHorarios = horaEntrada && horaSalida

  return (
    <form className="formulario-reserva" onSubmit={enviar}>
      <fieldset>
        <legend>Quien reserva</legend>
        {cargandoCuenta && <p className="suave">Cargando los datos de tu cuenta…</p>}
        {cuenta && (
          <div className="cuenta-reserva">
            <strong>{cuenta.nombre}</strong>
            <span>{TEXTO_DOCUMENTO[cuenta.tipoDocumento]} {cuenta.numeroDocumento}</span>
            <span>{cuenta.correo}</span>
            {cuenta.telefono && <span>{cuenta.telefono}</span>}
            <Link to="/mi-cuenta" className="suave">¿Algún dato cambió? Corrígelo en Mi cuenta</Link>
          </div>
        )}
        {!cargandoCuenta && !cuenta && (
          <p className="suave">
            A este correo te llega la confirmación. Con él y tu documento entras a ver tu reserva.
          </p>
        )}
        <div className="campos">
          {!cargandoCuenta && !cuenta && (
            <>
              <label>
                Nombres
                <input value={quien.nombres} maxLength={59} pattern=".*\S.*" required autoComplete="given-name"
                  onChange={(e) => setQuien((q) => ({ ...q, nombres: e.target.value }))} />
              </label>
              <label>
                Apellidos
                <input value={quien.apellidos} maxLength={60} pattern=".*\S.*" required autoComplete="family-name"
                  onChange={(e) => setQuien((q) => ({ ...q, apellidos: e.target.value }))} />
              </label>
            </>
          )}
          {meHospedo && !cargandoCuenta && (
            <label>
              Fecha de nacimiento
              <input type="date" value={personas[0].fechaNacimiento} max={hoy} required
                onChange={(e) => cambiarPersona(0, { fechaNacimiento: e.target.value })} />
            </label>
          )}
          {!cargandoCuenta && !cuenta && (
            <>
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
            </>
          )}
          {/* Una cuenta sin teléfono lo completa aquí: la reserva lo exige para recuperar la cuenta (DEC-68) */}
          {!cargandoCuenta && !cuenta?.telefono && <CampoTelefono alCambiar={setTelefono} />}
        </div>
        <label className="casilla">
          <input type="checkbox" checked={meHospedo} onChange={(e) => setMeHospedo(e.target.checked)} />
          Yo también me hospedo
        </label>
        {!meHospedo && (
          <p className="suave">
            Reservas para otras personas: respondes por la reserva y su pago, y abajo nos dices quiénes se hospedan.
          </p>
        )}
      </fieldset>

      {(meHospedo ? personas.length > 1 : true) && (
        <fieldset>
          <legend>{meHospedo ? 'Quienes viajan contigo' : 'Quienes se hospedan'}</legend>
          {personas.map((persona, i) => (meHospedo && i === 0 ? null : (
            <div className="campos" key={i} role="group" aria-label={`Persona ${i + 1}`}>
              <CampoPersona numero={i + 1} persona={persona} hoy={hoy}
                alCambiar={(cambio) => cambiarPersona(i, cambio)} />
            </div>
          )))}
        </fieldset>
      )}

      {(hayHorarios || hayExtras) && <fieldset>
        <legend>Tu estadía</legend>
        {hayHorarios && (
          <dl className="cuentas">
            <dt>Entrada desde</dt>
            <dd>{horaDelDia(horaEntrada)}</dd>
            <dt>Salida hasta</dt>
            <dd>{horaDelDia(horaSalida)}</dd>
          </dl>
        )}
        {hayExtras && (
          <details className="extras">
            <summary>¿Traes vehículo o mascota?</summary>
            <div className="campos">
              {(parametros?.cupoParqueadero ?? 0) > 0 && (
                <>
                  <CampoCantidad nombre="Carros" valor={carros} alCambiar={setCarros}
                    ayuda={ayudaParqueadero(cargoCarro, porNoche)} />
                  <CampoCantidad nombre="Motos" valor={motos} alCambiar={setMotos}
                    ayuda={ayudaParqueadero(cargoMoto, porNoche)} />
                </>
              )}
              {limiteMascotas > 0 && (
                <CampoCantidad nombre="Mascotas" valor={mascotas} alCambiar={setMascotas}
                  maximo={limiteMascotas}
                  ayuda={`Hasta ${limiteMascotas}${cargoMascota > 0
                    ? `; cada una suma ${enPesos(cargoMascota)} a tu total, una sola vez` : ''}`} />
              )}
            </div>
          </details>
        )}
      </fieldset>}

      {cambio && (
        <p className="aviso-cambio" role="status">
          Tu total cambió a <strong>{enPesos(cambio.total + totalExtras)}</strong>: según las fechas de
          nacimiento, pagan {pagan} {pagan === 1 ? 'persona' : 'personas'}. La edad que cuenta es la del día de
          llegada.
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

// DEC-89: gratis no necesita explicación
function ayudaParqueadero(cargo: number, porNoche: boolean) {
  if (cargo <= 0) return undefined
  return `Cada uno suma ${enPesos(cargo)} ${porNoche ? 'por noche' : 'por toda la estadía'}`
}

interface PropsCantidad {
  nombre: string
  valor: number
  maximo?: number
  ayuda?: string
  alCambiar: (valor: number) => void
}

function CampoCantidad({ nombre, valor, maximo, ayuda, alCambiar }: PropsCantidad) {
  const id = useId()
  return (
    <div className="campo">
      <label htmlFor={id}>{nombre}</label>
      <input id={id} type="number" inputMode="numeric" min={0} max={maximo} value={valor}
        aria-describedby={ayuda ? `${id}-ayuda` : undefined}
        onChange={(e) => alCambiar(Math.min(maximo ?? Infinity, Math.max(0, Number(e.target.value) || 0)))} />
      {ayuda && <span id={`${id}-ayuda`} className="ayuda">{ayuda}</span>}
    </div>
  )
}

interface PropsPersona {
  /** Su lugar en el grupo, para que cada campo tenga su nombre. */
  numero: number
  persona: Persona
  hoy: string
  alCambiar: (cambio: Partial<Persona>) => void
}

function CampoPersona({ numero, persona, hoy, alCambiar }: PropsPersona) {
  return (
    <>
      <label>
        {`Nombres de la persona ${numero}`}
        <input value={persona.nombres} maxLength={59} pattern=".*\S.*" required autoComplete="off"
          onChange={(e) => alCambiar({ nombres: e.target.value })} />
      </label>
      <label>
        {`Apellidos de la persona ${numero}`}
        <input value={persona.apellidos} maxLength={60} pattern=".*\S.*" required autoComplete="off"
          onChange={(e) => alCambiar({ apellidos: e.target.value })} />
      </label>
      <label>
        {`Fecha de nacimiento de la persona ${numero}`}
        <input type="date" value={persona.fechaNacimiento} max={hoy} required
          onChange={(e) => alCambiar({ fechaNacimiento: e.target.value })} />
      </label>
    </>
  )
}
