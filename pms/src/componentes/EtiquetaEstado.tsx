import type { ReactNode } from 'react'
import { NOMBRE_DEL_ESTADO, type EstadoReserva } from '../api/reservas'

// Una forma distinta por estado: el color acompaña, pero no es lo único que los separa
const FORMAS: Record<EstadoReserva, { clase: string; forma: ReactNode }> = {
  PENDIENTE: { clase: 'pendiente', forma: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" /> },
  CONFIRMADA: { clase: 'confirmada', forma: <circle cx="6" cy="6" r="3" className="relleno" /> },
  EN_CURSO: { clase: 'en-curso', forma: <path d="M6 1.5 10.5 6 6 10.5 1.5 6z" className="relleno" /> },
  FINALIZADA: { clase: 'finalizada', forma: <path d="M2 6.5 4.8 9.5 10 3" /> },
  CANCELADA: { clase: 'cancelada', forma: <path d="M2.5 2.5l7 7M9.5 2.5l-7 7" /> },
  NO_SHOW: { clase: 'no-show', forma: <circle cx="6" cy="6" r="4" /> },
}

/** El estado de una reserva, siempre con su nombre escrito. */
export default function EtiquetaEstado({ estado }: { estado: EstadoReserva }) {
  const { clase, forma } = FORMAS[estado]
  return (
    <span className={`etiqueta etiqueta-${clase}`}>
      <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
        {forma}
      </svg>
      {NOMBRE_DEL_ESTADO[estado]}
    </span>
  )
}
