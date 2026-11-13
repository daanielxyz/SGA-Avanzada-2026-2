// Textos de los rechazos del backend que recepción no entendería tal como llegan.
import { ErrorApi } from '../api/cliente'

export interface ErrorMostrado {
  mensaje: string
  detalles: string[]
}

/** Lo que se muestra cuando una acción falla: el rechazo de la API o, si no respondió, que se intente de nuevo. */
export function errorParaMostrar(causa: unknown): ErrorMostrado {
  return causa instanceof ErrorApi
    ? { mensaje: mensajeDeRechazo(causa), detalles: causa.detalles }
    : { mensaje: 'Inténtalo de nuevo en un momento.', detalles: [] }
}

// Los del folio traen códigos internos («El folio FOL-3 está cerrado», «El cargo CAR-5 ya fue corregido»)
const POR_MOTIVO: Record<string, string> = {
  ESTADO_NO_PERMITIDO:
    'Esta reserva ya cambió de estado; puede que otro turno la haya modificado. Revisa el detalle actualizado.',
  FOLIO_CERRADO: 'El folio ya está cerrado y no admite movimientos; puede que otro turno lo haya cerrado.',
  YA_REVERTIDO: 'Este movimiento ya fue revertido; puede que otro turno lo haya hecho. Revisa el folio actualizado.',
  SALDO_PENDIENTE: 'El folio ya no está en cero; puede que otro turno haya registrado un movimiento. Revisa el saldo.',
  YA_ATENDIDA: 'Otro turno ya la marcó como atendida.',
  LLEGADA_VENCIDA:
    'Ya pasó la hora límite de llegada y la reserva no se confirmó a tiempo. Si el huésped llegó, cancélala y haz una nueva.',
}

/**
 * El mensaje que se le muestra a recepción (DEC-94): si otro turno ya cambió la reserva o el folio, el del backend
 * solo dice qué regla falló. Los demás van tal cual.
 */
export function mensajeDeRechazo(error: ErrorApi): string {
  return (error.motivo && POR_MOTIVO[error.motivo]) || error.message
}
