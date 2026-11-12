// Titulares: quien responde por la reserva y su folio; puede no alojarse (DEC-97).
import { pedir } from './cliente'

/** Lo que usa el PMS de `TitularResponse`. */
export interface Titular {
  id: string
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  correo: string | null
  telefono: string | null
  /** Recepción revisó el documento en persona (DEC-98). */
  validado: boolean
  validadoEn: string | null
  /** El id del usuario que lo validó, no su nombre: por eso la pantalla solo muestra cuándo. */
  validadoPor: string | null
}

/**
 * `GET /api/titulares/{id}`.
 *
 * @throws ErrorApi 404 si no existe
 */
export function consultarTitular(id: string): Promise<Titular> {
  return pedir<Titular>(`/api/titulares/${encodeURIComponent(id)}`)
}

/**
 * `PUT /api/titulares/{id}/validar` (DEC-98): no se deshace, y desde ahí el huésped no cambia su nombre desde su cuenta.
 *
 * @throws ErrorApi 409 `IDENTIDAD_VALIDADA` si ya estaba validada
 */
export function validarTitular(id: string): Promise<Titular> {
  return pedir<Titular>(`/api/titulares/${encodeURIComponent(id)}/validar`, { method: 'PUT' })
}
