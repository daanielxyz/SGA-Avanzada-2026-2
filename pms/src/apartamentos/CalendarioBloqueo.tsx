import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import type { Bloqueo } from '../api/apartamentos'
import { consultarOcupacion } from '../api/ocupacion'
import { cantidad, fechaCorta, fechaHoy, sumarDias } from '../formato'
import { nochesNoLibres, primeraNoLibreDesde } from './formulario'

// Se puede ir hasta un año adelante, como el calendario de la web
const MESES_HACIA_ADELANTE = 12

export interface RangoBloqueo {
  /** La primera noche que no se vende. */
  inicio: string
  /** Exclusiva: el día en que vuelve a venderse. Vacía mientras se elige. */
  fin: string
}

const desdeTexto = (fecha: string) => new Date(`${fecha}T00:00:00Z`)
const primeroDelMes = (hoy: string, desplazamiento: number) => {
  const fecha = desdeTexto(hoy)
  return new Date(Date.UTC(fecha.getUTCFullYear(), fecha.getUTCMonth() + desplazamiento, 1)).toISOString().slice(0, 10)
}
const mesesEntre = (desde: string, hasta: string) => {
  const a = desdeTexto(desde)
  const b = desdeTexto(hasta)
  return (b.getUTCFullYear() - a.getUTCFullYear()) * 12 + b.getUTCMonth() - a.getUTCMonth()
}
const formatoMes = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' })
const nombreMes = (fecha: string) => formatoMes.format(desdeTexto(fecha)).replace(/^./, (l) => l.toUpperCase())
const nombreDia = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' })
// Empieza el domingo, como el calendario colombiano y el de la web
const DIAS = [['D', 'domingo'], ['L', 'lunes'], ['M', 'martes'], ['M', 'miércoles'], ['J', 'jueves'], ['V', 'viernes'],
  ['S', 'sábado']]

function semanasDe(primero: string) {
  const inicio = desdeTexto(primero)
  const dias: (string | null)[] = Array.from({ length: inicio.getUTCDay() }, () => null)
  for (let d = primero; desdeTexto(d).getUTCMonth() === inicio.getUTCMonth(); d = sumarDias(d, 1)) dias.push(d)
  while (dias.length % 7 !== 0) dias.push(null)
  return Array.from({ length: dias.length / 7 }, (_, i) => dias.slice(i * 7, i * 7 + 7))
}

/**
 * Elegir las noches de un bloqueo en un calendario, como en la web pero con la marca de Meridian. Un clic elige la
 * primera noche y el siguiente el día en que vuelve a venderse. Las noches pasadas no se eligen; las que ya tienen una
 * reserva activa o un bloqueo tampoco, porque la API rechaza un bloqueo encima de una reserva (BLO-02). Como en una
 * estancia, el día en que vuelve a venderse puede ser la llegada de otra reserva.
 */
export default function CalendarioBloqueo({ codigo, bloqueos, rango, invalido = false, alCambiar }: {
  codigo: string
  bloqueos: Bloqueo[]
  rango: RangoBloqueo
  invalido?: boolean
  alCambiar: (rango: RangoBloqueo) => void
}) {
  const hoy = fechaHoy()
  const [desplazamiento, setDesplazamiento] = useState(() =>
    Math.min(Math.max(rango.inicio ? mesesEntre(hoy, rango.inicio) : 0, 0), MESES_HACIA_ADELANTE - 1))
  const [reservadas, setReservadas] = useState<Map<string, true>>(new Map())
  const [leidos, setLeidos] = useState<Set<string>>(new Set())
  const [problema, setProblema] = useState(false)
  const [enfocada, setEnfocada] = useState(rango.inicio || hoy)
  const moverFoco = useRef(false)
  const contenedor = useRef<HTMLDivElement>(null)

  const mes = primeroDelMes(hoy, desplazamiento)
  const desde = mes < hoy ? hoy : mes
  const hasta = primeroDelMes(hoy, desplazamiento + 1)

  // Las reservas de este apartamento en el mes que se ve (el tablero de recepción, solo las activas cuentan)
  useEffect(() => {
    let vigente = true
    consultarOcupacion({ desde, hasta, apartamento: codigo, pagina: 0 })
      .then((pagina) => {
        if (!vigente) return
        const fila = pagina.contenido.find((a) => a.apartamento === codigo)
        const activas = (fila?.reservas ?? []).filter((r) => ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'].includes(r.estado))
        setProblema(false)
        setReservadas((actuales) => new Map([...actuales, ...nochesNoLibres(activas.map((r) =>
          ({ inicio: r.entrada, fin: r.salida })), desde, hasta).map((d) => [d, true] as const)]))
        setLeidos((actuales) => new Set([...actuales, mes]))
      })
      .catch(() => vigente && setProblema(true))
    return () => {
      vigente = false
    }
  }, [codigo, desde, hasta, mes])

  const bloqueadas = new Set(nochesNoLibres(bloqueos.filter((b) => b.vigente)
    .map((b) => ({ inicio: b.fechaInicio, fin: b.fechaFin })), hoy, sumarDias(hoy, 400)))
  const ocupada = (fecha: string) => reservadas.has(fecha) || bloqueadas.has(fecha)
  const eligiendoFin = rango.inicio !== '' && rango.fin === ''
  // Sin leer las reservas no se sabe hasta dónde llega: si falló la lectura, la API decide al enviar
  const limiteFin = eligiendoFin ? primeraNoLibreDesde(sumarDias(rango.inicio, 1), ocupada, sumarDias(hoy, 400)) : null

  function estadoDe(fecha: string): { elegible: boolean; texto: string } {
    if (fecha < hoy) return { elegible: false, texto: 'ya pasó' }
    if (eligiendoFin && fecha > rango.inicio) {
      if (limiteFin !== null && fecha > limiteFin) return { elegible: false, texto: 'hay una reserva o un bloqueo antes' }
      return { elegible: true, texto: ocupada(fecha) ? 'solo como día en que vuelve a venderse' : 'vuelve a venderse' }
    }
    if (reservadas.has(fecha)) return { elegible: false, texto: 'tiene una reserva' }
    if (bloqueadas.has(fecha)) return { elegible: false, texto: 'ya está bloqueada' }
    if (!problema && !leidos.has(mes)) return { elegible: false, texto: 'cargando' }
    return { elegible: true, texto: 'libre' }
  }

  function elegir(fecha: string) {
    if (!estadoDe(fecha).elegible) return
    setEnfocada(fecha)
    alCambiar(eligiendoFin && fecha > rango.inicio ? { inicio: rango.inicio, fin: fecha } : { inicio: fecha, fin: '' })
  }

  const dias = semanasDe(mes).flat().filter((d): d is string => d !== null && d >= hoy)
  const conTabulador = dias.includes(enfocada) ? enfocada : dias[0]

  useEffect(() => {
    if (!moverFoco.current) return
    moverFoco.current = false
    contenedor.current?.querySelector<HTMLButtonElement>(`[data-fecha="${conTabulador}"]`)?.focus({ preventScroll: true })
  }, [conTabulador, desplazamiento])

  function alTeclear(evento: KeyboardEvent<HTMLButtonElement>, fecha: string) {
    const pasos: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
    const paso = pasos[evento.key]
    if (paso === undefined) return
    evento.preventDefault()
    const destino = sumarDias(fecha, paso)
    const mesDestino = mesesEntre(hoy, destino)
    if (destino < hoy || mesDestino >= MESES_HACIA_ADELANTE) return
    if (mesDestino !== desplazamiento) setDesplazamiento(mesDestino)
    moverFoco.current = true
    setEnfocada(destino)
  }

  return (
    // Con un error, el foco llega aquí (enfocarPrimerError busca aria-invalid) y el lector vuelve a leer la ayuda
    <div className={invalido ? 'calendario-bloqueo invalido' : 'calendario-bloqueo'} ref={contenedor}
      role="group" aria-label="Calendario del bloqueo" aria-invalid={invalido} tabIndex={-1}>
      <p className="calendario-ayuda" aria-live="polite">{ayuda(rango)}</p>
      <div className="calendario-cabecera">
        <button type="button" className="boton boton-secundario boton-chico" aria-label="Mes anterior"
          disabled={desplazamiento === 0} onClick={() => setDesplazamiento((d) => d - 1)}>
          <span aria-hidden="true">←</span>
        </button>
        <strong className="calendario-mes-nombre">{nombreMes(mes)}</strong>
        <button type="button" className="boton boton-secundario boton-chico" aria-label="Mes siguiente"
          disabled={desplazamiento >= MESES_HACIA_ADELANTE - 1} onClick={() => setDesplazamiento((d) => d + 1)}>
          <span aria-hidden="true">→</span>
        </button>
      </div>
      {problema && (
        <p className="campo-pista">No pudimos revisar las reservas de este mes. Puedes elegir igual: si hay una reserva, se te avisará al crear.</p>
      )}
      <table className="calendario-tabla">
        <caption className="solo-lectores">{nombreMes(mes)}</caption>
        <thead>
          <tr>{DIAS.map(([corto, largo]) => <th key={largo} scope="col"><abbr title={largo}>{corto}</abbr></th>)}</tr>
        </thead>
        <tbody>
          {semanasDe(mes).map((semana) => (
            <tr key={semana.find(Boolean) ?? mes}>
              {semana.map((fecha, i) => {
                if (!fecha) return <td key={`vacio-${i}`} />
                const { elegible, texto } = estadoDe(fecha)
                const extremo = fecha === rango.inicio || fecha === rango.fin
                const enRango = rango.fin !== '' && fecha > rango.inicio && fecha < rango.fin
                const clases = ['dia', reservadas.has(fecha) && 'reservada', bloqueadas.has(fecha) && 'bloqueada',
                  enRango && 'en-rango', extremo && 'extremo', fecha === hoy && 'hoy'].filter(Boolean).join(' ')
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
      <ul className="calendario-leyenda" aria-label="Qué significa cada día">
        <li><span className="muestra muestra-reservada" aria-hidden="true" />Con reserva</li>
        <li><span className="muestra muestra-bloqueada" aria-hidden="true" />Ya bloqueada</li>
        <li><span className="muestra muestra-elegida" aria-hidden="true" />Este bloqueo</li>
      </ul>
    </div>
  )
}

function ayuda({ inicio, fin }: RangoBloqueo) {
  if (inicio && fin) {
    const n = Math.round((desdeTexto(fin).getTime() - desdeTexto(inicio).getTime()) / 864e5)
    return `Del ${fechaCorta(inicio)} al ${fechaCorta(fin)} · ${cantidad(n, 'noche', 'noches')} sin vender`
  }
  if (inicio) return `Desde el ${fechaCorta(inicio)}. Ahora elige el día en que vuelve a venderse.`
  return 'Elige la primera noche que no se vende.'
}
