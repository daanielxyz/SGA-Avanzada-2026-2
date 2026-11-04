import { NOMBRE_DEL_ESTADO, type EstadoReserva } from '../api/reservas'
import { claseDeEstado, formaDeEstado } from './estadosReserva'

/** Solo la forma, para quien ya escribe el nombre del estado aparte. */
export function FormaEstado({ estado }: { estado: EstadoReserva }) {
  return (
    <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
      {formaDeEstado(estado)}
    </svg>
  )
}

/** El estado de una reserva, siempre con su nombre escrito. */
export default function EtiquetaEstado({ estado }: { estado: EstadoReserva }) {
  return (
    <span className={`etiqueta etiqueta-${claseDeEstado(estado)}`}>
      <FormaEstado estado={estado} />
      {NOMBRE_DEL_ESTADO[estado] ?? estado}
    </span>
  )
}
