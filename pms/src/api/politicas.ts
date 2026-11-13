// Política de cancelación congelada en una reserva (RN-13 · DEC-104): recepción le explica al huésped cuánto se retiene.
import { pedir } from './cliente'

export interface Penalizacion {
  /** VALOR_TOTAL (el valor congelado de la estancia) o PAGADO (lo pagado neto en el folio). */
  base: string
  /** Vacío si es un monto fijo. */
  porcentaje: number | null
  /** Vacío si es un porcentaje. */
  montoFijo: number | null
}

export interface Tramo {
  /** Desde cuántas horas antes de la entrada rige este tramo. */
  antelacionMinHoras: number
  penalizacion: Penalizacion
}

/** Lo que usa el PMS de `PoliticaResponse`. No trae nombre. */
export interface Politica {
  id: string
  version: number
  tramos: Tramo[]
  penalizacionNoShow: Penalizacion
}

/**
 * `GET /api/politicas/{id}`: la versión congelada en la reserva, aunque ya no sea la vigente.
 *
 * @throws ErrorApi 404 si no existe
 */
export function consultarPolitica(id: string): Promise<Politica> {
  return pedir<Politica>(`/api/politicas/${encodeURIComponent(id)}`)
}
