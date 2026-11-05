import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { consultarCalendario, type Estadia } from '../api/apartamentos'
import { fechaCorta, fechaDesdeHoy, masDias, noches as contarNoches } from '../formato'

// Se puede avanzar hasta un año: lo mismo que mira la próxima noche libre
const MESES_HACIA_ADELANTE = 12

interface Props {
  /** Con un apartamento, tacha sus noches ocupadas (CU-08); sin él, como en la búsqueda, todas se pueden elegir. */
  codigo?: string
  /** Una fecha vacía es una fecha por elegir: con llegada y sin salida, el próximo clic es la salida. */
  estadia: Estadia
  alCambiar: (estadia: Estadia) => void
  /** Dos en pantallas anchas, uno en el celular. */
  mesesVisibles: number
}

// Las fechas van como texto AAAA-MM-DD y se calculan en UTC, para que la zona horaria no corra un día
const aTexto = (fecha: Date) => fecha.toISOString().slice(0, 10)
const desdeTexto = (fecha: string) => new Date(`${fecha}T00:00:00Z`)

function primeroDelMes(hoy: string, desplazamiento: number) {
  const fecha = desdeTexto(hoy)
  return aTexto(new Date(Date.UTC(fecha.getUTCFullYear(), fecha.getUTCMonth() + desplazamiento, 1)))
}

function mesesEntre(desde: string, hasta: string) {
  const a = desdeTexto(desde)
  const b = desdeTexto(hasta)
  return (b.getUTCFullYear() - a.getUTCFullYear()) * 12 + b.getUTCMonth() - a.getUTCMonth()
}

const formatoMes = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' })
// «noviembre de 2026» → «Noviembre de 2026»: solo la primera letra, no cada palabra
const nombreMes = (fecha: Date) => formatoMes.format(fecha).replace(/^./, (l) => l.toUpperCase())
const nombreDia = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
// Empieza el domingo, como el calendario colombiano
const DIAS = [['D', 'domingo'], ['L', 'lunes'], ['M', 'martes'], ['M', 'miércoles'], ['J', 'jueves'], ['V', 'viernes'],
  ['S', 'sábado']]

/** Las semanas de un mes, con huecos vacíos antes del día 1 para que cada día caiga en su columna. */
function semanasDe(primero: string) {
  const inicio = desdeTexto(primero)
  const dias: (string | null)[] = Array.from({ length: inicio.getUTCDay() }, () => null)
  for (let d = primero; desdeTexto(d).getUTCMonth() === inicio.getUTCMonth(); d = masDias(d, 1)) dias.push(d)
  while (dias.length % 7 !== 0) dias.push(null)
  return Array.from({ length: dias.length / 7 }, (_, i) => dias.slice(i * 7, i * 7 + 7))
}

/**
 * Un clic elige la llegada y el siguiente la salida. La noche de la salida no se ocupa (EST-01): por eso la primera
 * noche ocupada después de la llegada todavía sirve como salida, y ninguna después.
 */
export default function Calendario({ codigo, estadia, alCambiar, mesesVisibles }: Props) {
  const hoy = fechaDesdeHoy()
  const { entrada, salida } = estadia
  const maximoDesplazamiento = MESES_HACIA_ADELANTE - mesesVisibles
  const [desplazamiento, setDesplazamiento] = useState(() =>
    Math.min(Math.max(entrada ? mesesEntre(hoy, entrada) : 0, 0), maximoDesplazamiento))
  const [noches, setNoches] = useState<Map<string, boolean>>(new Map())
  const [problema, setProblema] = useState(false)
  const [enfocada, setEnfocada] = useState(entrada && entrada >= hoy ? entrada : hoy)
  const moverFoco = useRef(true) // al abrir, el foco va al día de la llegada
  const contenedor = useRef<HTMLDivElement>(null)

  const meses = Array.from({ length: mesesVisibles }, (_, i) => primeroDelMes(hoy, desplazamiento + i))
  const desde = meses[0] < hoy ? hoy : meses[0]
  const hasta = primeroDelMes(hoy, desplazamiento + mesesVisibles)

  // Se pide cada vez que se abre: así refleja las reservas que vencieron o se tomaron mientras tanto
  useEffect(() => {
    if (!codigo) return
    let vigente = true
    consultarCalendario(codigo, desde, hasta)
      .then((lista) => {
        if (!vigente) return
        setProblema(false)
        setNoches((actuales) => new Map([...actuales, ...lista.map((n) => [n.fecha, n.libre] as const)]))
      })
      .catch(() => vigente && setProblema(true))
    return () => {
      vigente = false
    }
  }, [codigo, desde, hasta])

  const visibles = meses.flatMap((m) => semanasDe(m).flat()).filter((d): d is string => d !== null && d >= hoy)
  // Si el día enfocado no se ve, el primero visible recibe el tabulador: el calendario siempre es alcanzable
  const conTabulador = visibles.includes(enfocada) ? enfocada : visibles[0]

  useEffect(() => {
    if (!moverFoco.current) return
    moverFoco.current = false
    // Sin desplazar la página: de mostrar el calendario entero se encarga quien lo abre
    contenedor.current?.querySelector<HTMLButtonElement>(`[data-fecha="${conTabulador}"]`)
      ?.focus({ preventScroll: true })
  }, [conTabulador, desplazamiento])

  const eligiendoSalida = entrada !== '' && salida === ''
  const libre = (fecha: string) => (codigo ? noches.get(fecha) : true)
  const limiteSalida = eligiendoSalida && codigo ? primeraNoLibreDesde(masDias(entrada, 1), noches, hasta) : null

  function estadoDe(fecha: string) {
    if (fecha < hoy) return { elegible: false, texto: 'ya pasó' }
    if (eligiendoSalida && fecha > entrada) {
      const sirve = limiteSalida === null || fecha <= limiteSalida
      if (!sirve) return { elegible: false, texto: 'no disponible' }
      return { elegible: true, texto: libre(fecha) === false ? 'solo salida' : 'disponible como salida' }
    }
    if (libre(fecha) === undefined) return { elegible: false, texto: 'cargando' }
    return libre(fecha) ? { elegible: true, texto: 'libre' } : { elegible: false, texto: 'no disponible' }
  }

  function elegir(fecha: string) {
    if (!estadoDe(fecha).elegible) return
    setEnfocada(fecha)
    // Con la llegada puesta, un día posterior es la salida; cualquier otro empieza de nuevo
    alCambiar(eligiendoSalida && fecha > entrada ? { entrada, salida: fecha } : { entrada: fecha, salida: '' })
  }

  function alTeclear(evento: KeyboardEvent<HTMLButtonElement>, fecha: string) {
    const pasos: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
    const paso = pasos[evento.key]
    if (paso === undefined) return
    evento.preventDefault()
    const destino = masDias(fecha, paso)
    const mes = mesesEntre(hoy, destino)
    if (destino < hoy || mes >= MESES_HACIA_ADELANTE) return
    // Si el día queda fuera de lo que se ve, se corre el calendario
    if (mes < desplazamiento) setDesplazamiento(mes)
    if (mes >= desplazamiento + mesesVisibles) setDesplazamiento(mes - mesesVisibles + 1)
    moverFoco.current = true
    setEnfocada(destino)
  }

  return (
    <div className="calendario" ref={contenedor}>
      <p className="calendario-ayuda" aria-live="polite">
        {ayuda(estadia, Boolean(codigo))}
      </p>
      <div className="calendario-navegacion">
        <button type="button" className="secundario" disabled={desplazamiento === 0}
          onClick={() => setDesplazamiento((d) => d - 1)}>
          ← Anterior
        </button>
        <button type="button" className="secundario" disabled={desplazamiento >= maximoDesplazamiento}
          onClick={() => setDesplazamiento((d) => d + 1)}>
          Siguiente →
        </button>
      </div>
      {problema && <p className="aviso">No pudimos revisar qué noches están libres. Inténtalo de nuevo.</p>}
      <div className="calendario-meses" data-meses={mesesVisibles}>
        {meses.map((mes) => (
          <table key={mes} className="calendario-mes">
            <caption>{nombreMes(desdeTexto(mes))}</caption>
            <thead>
              <tr>
                {DIAS.map(([corto, largo]) => <th key={largo} scope="col"><abbr title={largo}>{corto}</abbr></th>)}
              </tr>
            </thead>
            <tbody>
              {semanasDe(mes).map((semana) => (
                <tr key={semana.find(Boolean) ?? mes}>
                  {semana.map((fecha, i) => {
                    if (!fecha) return <td key={`vacio-${i}`} />
                    const { elegible, texto } = estadoDe(fecha)
                    const extremo = fecha === entrada || fecha === salida
                    const enRango = salida !== '' && fecha > entrada && fecha < salida
                    const clases = ['dia', libre(fecha) === false && 'no-libre', enRango && 'en-rango',
                      extremo && 'extremo', fecha === hoy && 'hoy'].filter(Boolean).join(' ')
                    return (
                      <td key={fecha}>
                        <button type="button" className={clases} data-fecha={fecha} aria-disabled={!elegible}
                          aria-pressed={extremo} tabIndex={fecha === conTabulador ? 0 : -1}
                          aria-label={`${nombreDia.format(desdeTexto(fecha))}, ${texto}`}
                          onClick={() => elegir(fecha)} onKeyDown={(e) => alTeclear(e, fecha)}>
                          {desdeTexto(fecha).getUTCDate()}
                        </button>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        ))}
      </div>
    </div>
  )
}

function ayuda({ entrada, salida }: Estadia, conDisponibilidad: boolean) {
  if (entrada && salida) {
    const n = contarNoches(entrada, salida)
    return `${fechaCorta(entrada)} – ${fechaCorta(salida)} · ${n} ${n === 1 ? 'noche' : 'noches'}`
  }
  if (entrada) return `Llegada el ${fechaCorta(entrada)}. Ahora elige la salida.`
  return conDisponibilidad ? 'Elige la llegada. Las fechas tachadas no están libres.' : 'Elige la llegada.'
}

/** La primera noche no libre desde esa fecha, o vacía si todas lo están. Lo que no se conoce cuenta como no libre. */
function primeraNoLibreDesde(desde: string, noches: Map<string, boolean>, hasta: string) {
  for (let d = desde; d < hasta; d = masDias(d, 1)) {
    if (noches.get(d) !== true) return d
  }
  return null
}
