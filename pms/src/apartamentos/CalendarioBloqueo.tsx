import { useEffect, useState } from 'react'
import type { Bloqueo } from '../api/apartamentos'
import { consultarOcupacion } from '../api/ocupacion'
import Calendario, { type EstadoDia } from '../componentes/Calendario'
import { mesesEntre, primeroDelMes } from '../componentes/fechasCalendario'
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

/**
 * Elegir las noches de un bloqueo en el calendario del PMS (`componentes/Calendario`). Un clic elige la primera noche
 * y el siguiente el día en que vuelve a venderse. Las noches pasadas no se eligen; las que ya tienen una reserva activa
 * o un bloqueo tampoco, porque la API rechaza un bloqueo encima de una reserva (BLO-02). Como en una estancia, el día
 * en que vuelve a venderse puede ser la llegada de otra reserva.
 */
export default function CalendarioBloqueo({ codigo, bloqueos, rango, invalido = false, alCambiar }: {
  codigo: string
  bloqueos: Bloqueo[]
  rango: RangoBloqueo
  invalido?: boolean
  alCambiar: (rango: RangoBloqueo) => void
}) {
  const hoy = fechaHoy()
  const primerMes = primeroDelMes(hoy)
  const ultimoMes = primeroDelMes(hoy, MESES_HACIA_ADELANTE - 1)
  const [mes, setMes] = useState(() =>
    primeroDelMes(hoy, Math.min(Math.max(rango.inicio ? mesesEntre(hoy, rango.inicio) : 0, 0), MESES_HACIA_ADELANTE - 1)))
  const [reservadas, setReservadas] = useState<Map<string, true>>(new Map())
  const [leidos, setLeidos] = useState<Set<string>>(new Set())
  const [problema, setProblema] = useState(false)

  const desde = mes < hoy ? hoy : mes
  const hasta = primeroDelMes(mes, 1)

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

  function estadoDe(fecha: string): EstadoDia {
    const clases = [reservadas.has(fecha) && 'reservada', bloqueadas.has(fecha) && 'bloqueada']
    if (fecha < hoy) return { elegible: false, texto: 'ya pasó', clases }
    if (eligiendoFin && fecha > rango.inicio) {
      if (limiteFin !== null && fecha > limiteFin) return { elegible: false, texto: 'hay una reserva o un bloqueo antes', clases }
      return { elegible: true, texto: ocupada(fecha) ? 'solo como día en que vuelve a venderse' : 'vuelve a venderse', clases }
    }
    if (reservadas.has(fecha)) return { elegible: false, texto: 'tiene una reserva', clases }
    if (bloqueadas.has(fecha)) return { elegible: false, texto: 'ya está bloqueada', clases }
    if (!problema && !leidos.has(mes)) return { elegible: false, texto: 'cargando', clases }
    return { elegible: true, texto: 'libre', clases }
  }

  return (
    <Calendario
      etiqueta="Calendario del bloqueo"
      mes={mes}
      alCambiarMes={setMes}
      primerMes={primerMes}
      ultimoMes={ultimoMes}
      estadoDe={estadoDe}
      inicio={rango.inicio}
      fin={rango.fin}
      invalido={invalido}
      alElegir={(fecha) => alCambiar(eligiendoFin && fecha > rango.inicio
        ? { inicio: rango.inicio, fin: fecha }
        : { inicio: fecha, fin: '' })}
      ayuda={ayuda(rango)}
      aviso={problema && (
        <p className="campo-pista">No pudimos revisar las reservas de este mes. Puedes elegir igual: si hay una reserva, se te avisará al crear.</p>
      )}
      leyenda={(
        <ul className="calendario-leyenda" aria-label="Qué significa cada día">
          <li><span className="muestra muestra-reservada" aria-hidden="true" />Con reserva</li>
          <li><span className="muestra muestra-bloqueada" aria-hidden="true" />Ya bloqueada</li>
          <li><span className="muestra muestra-elegida" aria-hidden="true" />Este bloqueo</li>
        </ul>
      )}
    />
  )
}

function ayuda({ inicio, fin }: RangoBloqueo) {
  if (inicio && fin) {
    const n = Math.round((new Date(`${fin}T00:00:00Z`).getTime() - new Date(`${inicio}T00:00:00Z`).getTime()) / 864e5)
    return `Del ${fechaCorta(inicio)} al ${fechaCorta(fin)} · ${cantidad(n, 'noche', 'noches')} sin vender`
  }
  if (inicio) return `Desde el ${fechaCorta(inicio)}. Ahora elige el día en que vuelve a venderse.`
  return 'Elige la primera noche que no se vende.'
}
