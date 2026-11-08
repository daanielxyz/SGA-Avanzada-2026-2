// Textos de los rechazos del backend que recepción no entendería tal como llegan.
import type { ErrorApi } from '../api/cliente'

/**
 * El mensaje que se le muestra a recepción (DEC-94): si otro turno ya cambió la reserva, el del backend solo dice qué
 * transición falló («CANCELADA → CANCELADA»). Los demás van tal cual.
 */
export function mensajeDeRechazo(error: ErrorApi): string {
  if (error.motivo === 'ESTADO_NO_PERMITIDO') {
    return 'Esta reserva ya cambió de estado; puede que otro turno la haya modificado. Revisa el detalle actualizado.'
  }
  return error.message
}
