// Política de cancelación congelada en una reserva (RN-13 · DEC-104): recepción le explica al huésped cuánto se retiene.
import { pedir } from './cliente'

export interface Penalizacion {
  /** VALOR_TOTAL (el valor congelado de la estancia), PAGADO (lo pagado neto en el folio) o ANTICIPO_EXIGIDO. */
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

/** Página de versiones de `GET /api/politicas`: de la más alta a la más baja, con la vigente incluida (POL-04). */
export interface PaginaPoliticas {
  contenido: Politica[]
  numero: number
  totalElementos: number
  totalPaginas: number
}

export type BaseRetencion = 'VALOR_TOTAL' | 'PAGADO' | 'ANTICIPO_EXIGIDO'

/** `PublicarPoliticaRequest`: cada retención es un porcentaje o un monto fijo, nunca los dos (POL-03). */
export interface PoliticaNueva {
  tramos: { antelacionMinHoras: number; penalizacion: PenalizacionNueva }[]
  penalizacionNoShow: PenalizacionNueva
}

export interface PenalizacionNueva {
  base: BaseRetencion
  porcentaje?: number
  montoFijo?: number
}

/** `GET /api/politicas?pagina=` (solo el administrador). */
export function listarPoliticas(pagina: number): Promise<PaginaPoliticas> {
  return pedir<PaginaPoliticas>(`/api/politicas?pagina=${pagina}`)
}

/**
 * `GET /api/politicas/vigente`.
 *
 * @throws ErrorApi 404 si el alojamiento todavía no tiene política
 */
export function consultarPoliticaVigente(): Promise<Politica> {
  return pedir<Politica>('/api/politicas/vigente')
}

/** `POST /api/politicas`: la versión nueva rige para las reservas que se creen desde ahora (POL-04). */
export function publicarPolitica(politica: PoliticaNueva): Promise<Politica> {
  return pedir<Politica>('/api/politicas', { method: 'POST', cuerpo: politica })
}
