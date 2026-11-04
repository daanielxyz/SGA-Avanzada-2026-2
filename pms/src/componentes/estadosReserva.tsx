import type { ReactNode } from 'react'
import type { EstadoReserva } from '../api/reservas'

// Una forma distinta por estado: el color acompaña, pero no es lo único que los separa
const FORMAS: Partial<Record<string, { clase: string; forma: ReactNode }>> = {
  PENDIENTE: { clase: 'pendiente', forma: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" /> },
  CONFIRMADA: { clase: 'confirmada', forma: <circle cx="6" cy="6" r="3" className="relleno" /> },
  EN_CURSO: { clase: 'en-curso', forma: <path d="M6 1.5 10.5 6 6 10.5 1.5 6z" className="relleno" /> },
  FINALIZADA: { clase: 'finalizada', forma: <path d="M2 6.5 4.8 9.5 10 3" /> },
  CANCELADA: { clase: 'cancelada', forma: <path d="M2.5 2.5l7 7M9.5 2.5l-7 7" /> },
  NO_SHOW: { clase: 'no-show', forma: <circle cx="6" cy="6" r="4" /> },
}

// Un estado que el PMS todavía no conoce se muestra tal cual llega, en vez de romper la pantalla
const SIN_FORMA = { clase: 'finalizada', forma: <circle cx="6" cy="6" r="2" className="relleno" /> }

/** La clase de color de un estado: `etiqueta-…` pinta su forma y `barra-…`, las barras del tablero. */
export function claseDeEstado(estado: EstadoReserva): string {
  return (FORMAS[estado] ?? SIN_FORMA).clase
}

export function formaDeEstado(estado: EstadoReserva): ReactNode {
  return (FORMAS[estado] ?? SIN_FORMA).forma
}
