import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { consultarCalendario, type Estadia } from '../api/apartamentos'
import { fechaCorta, fechaDesdeHoy, masDias } from '../formato'

// Se ven dos meses y se puede avanzar hasta un año: lo mismo que mira la próxima noche libre
const MESES_VISIBLES = 2
const MESES_HACIA_ADELANTE = 12

interface Props {
  codigo: string
  estadia: Estadia
  alCambiar: (estadia: Estadia) => void
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
 * Las noches libres de un apartamento (CU-08), para elegir la llegada y la salida con un clic cada una. La noche de la
 * salida no se ocupa (EST-01): por eso la primera noche ocupada después de la llegada todavía sirve como salida.
 */
export default function CalendarioDisponibilidad({ codigo, estadia, alCambiar }: Props) {
  const hoy = fechaDesdeHoy()
  const [desplazamiento, setDesplazamiento] = useState(() =>
    Math.min(Math.max(mesesEntre(hoy, estadia.entrada), 0), MESES_HACIA_ADELANTE - MESES_VISIBLES))
  const [noches, setNoches] = useState<Map<string, boolean>>(new Map())
  const [problema, setProblema] = useState(false)
  // La llegada que se acaba de elegir aquí: mientras siga siendo la de la página, el próximo clic es la salida
  const [llegadaElegida, setLlegadaElegida] = useState<string | null>(null)
  const [enfocada, setEnfocada] = useState(estadia.entrada)
  const moverFoco = useRef(false)
  const contenedor = useRef<HTMLDivElement>(null)

  const primerMes = primeroDelMes(hoy, desplazamiento)
  const meses = Array.from({ length: MESES_VISIBLES }, (_, i) => primeroDelMes(hoy, desplazamiento + i))
  const desde = primerMes < hoy ? hoy : primerMes
  const hasta = primeroDelMes(hoy, desplazamiento + MESES_VISIBLES)

  useEffect(() => {
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

  useEffect(() => {
    if (!moverFoco.current) return
    moverFoco.current = false
    contenedor.current?.querySelector<HTMLButtonElement>(`[data-fecha="${enfocada}"]`)?.focus()
  }, [enfocada, desplazamiento])

  const eligiendoSalida = llegadaElegida !== null && llegadaElegida === estadia.entrada
  // La salida puede caer en la primera noche ocupada, nunca después: no se vende una estadía con una noche ajena
  const limiteSalida = eligiendoSalida ? primeraNoLibreDesde(masDias(estadia.entrada, 1), noches, hasta) : null

  function estadoDe(fecha: string) {
    const libre = noches.get(fecha)
    if (fecha < hoy) return { elegible: false, texto: 'ya pasó' }
    if (eligiendoSalida && fecha > estadia.entrada) {
      const sirve = limiteSalida === null || fecha <= limiteSalida
      return sirve
        ? { elegible: true, texto: libre === false ? 'solo salida' : 'disponible como salida' }
        : { elegible: false, texto: 'no disponible' }
    }
    if (libre === undefined) return { elegible: false, texto: 'cargando' }
    return libre ? { elegible: true, texto: 'libre' } : { elegible: false, texto: 'no disponible' }
  }

  function elegir(fecha: string) {
    if (!estadoDe(fecha).elegible) return
    setEnfocada(fecha)
    if (eligiendoSalida && fecha > estadia.entrada) {
      setLlegadaElegida(null)
      alCambiar({ entrada: estadia.entrada, salida: fecha })
      return
    }
    // Mientras se elige la salida, la página ya muestra el precio de una noche
    setLlegadaElegida(fecha)
    alCambiar({ entrada: fecha, salida: masDias(fecha, 1) })
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
    if (mes >= desplazamiento + MESES_VISIBLES) setDesplazamiento(mes - MESES_VISIBLES + 1)
    moverFoco.current = true
    setEnfocada(destino)
  }

  // Si el día enfocado no se ve, el primero visible recibe el tabulador: el calendario siempre es alcanzable
  const visibles = meses.flatMap((m) => semanasDe(m).flat()).filter((d): d is string => d !== null && d >= hoy)
  const conTabulador = visibles.includes(enfocada) ? enfocada : visibles[0]

  return (
    <div className="calendario" ref={contenedor}>
      <p className="suave calendario-ayuda" aria-live="polite">
        {eligiendoSalida
          ? `Llegada el ${fechaCorta(estadia.entrada)}. Ahora elige la salida.`
          : 'Elige la llegada y luego la salida. Las fechas tachadas no están libres.'}
      </p>
      <div className="calendario-navegacion">
        <button type="button" className="secundario" disabled={desplazamiento === 0}
          onClick={() => setDesplazamiento((d) => d - 1)}>
          ← Mes anterior
        </button>
        <button type="button" className="secundario" disabled={desplazamiento >= MESES_HACIA_ADELANTE - MESES_VISIBLES}
          onClick={() => setDesplazamiento((d) => d + 1)}>
          Mes siguiente →
        </button>
      </div>
      {problema && <p className="aviso">No pudimos cargar las noches libres. Puedes elegir las fechas arriba.</p>}
      <div className="calendario-meses">
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
                    const extremo = fecha === estadia.entrada || (!eligiendoSalida && fecha === estadia.salida)
                    const enRango = !eligiendoSalida && fecha > estadia.entrada && fecha < estadia.salida
                    const clases = ['dia', noches.get(fecha) === false && 'no-libre', enRango && 'en-rango',
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

/** La primera noche no libre desde esa fecha, o vacía si todas lo están. Lo que no se conoce cuenta como no libre. */
function primeraNoLibreDesde(desde: string, noches: Map<string, boolean>, hasta: string) {
  for (let d = desde; d < hasta; d = masDias(d, 1)) {
    if (noches.get(d) !== true) return d
  }
  return null
}
