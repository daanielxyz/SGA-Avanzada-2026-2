// Titulares: quien responde por la reserva y su folio; puede no alojarse (DEC-97).
import { pedir } from './cliente'
import type { Pagina } from './reservas'

/** Lo que usa el PMS de `TitularResponse`. */
export interface Titular {
  id: string
  /** Nombres y apellidos juntos. */
  nombre: string
  tipoDocumento: string
  numeroDocumento: string
  correo: string | null
  telefono: string | null
  /** El nombre por partes (DEC-98). Un titular de antes puede traer todo en `nombres` y los apellidos vacíos. */
  nombres?: string | null
  primerApellido?: string | null
  segundoApellido?: string | null
  /** Recepción revisó el documento en persona (DEC-98). */
  validado: boolean
  validadoEn: string | null
  /** El id del usuario que lo validó. */
  validadoPor: string | null
  /** Su nombre vigente; nulo si no está validado o esa cuenta ya no existe (DEC-115). */
  validadoPorNombre?: string | null
}

/** `ActualizarTitularRequest`: el nombre por partes y el contacto como quedan. El documento no viaja (TIT-05). */
export interface DatosTitular {
  nombres: string
  primerApellido: string | null
  segundoApellido: string | null
  correo: string | null
  telefono: string | null
}

/**
 * `GET /api/titulares?buscar=&pagina=` (DEC-131): cada palabra en el nombre o en el documento, sin importar
 * mayúsculas, tildes ni los puntos del número; sin texto, todos. Por nombre, de 10 en 10.
 *
 * @throws ErrorApi 400 si el texto pasa de 100 caracteres
 */
export function buscarTitulares(texto: string, pagina: number): Promise<Pagina<Titular>> {
  const consulta = new URLSearchParams({ pagina: String(pagina) })
  if (texto.trim()) consulta.set('buscar', texto.trim())
  return pedir<Pagina<Titular>>(`/api/titulares?${consulta}`)
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
 * `PUT /api/titulares/{id}`: el hotel cambia su nombre y su contacto, también con la identidad validada (DEC-98).
 *
 * @throws ErrorApi 409 sin ningún contacto o sin apellido (DEC-49); 400 si un dato no tiene el formato
 */
export function actualizarTitular(id: string, datos: DatosTitular): Promise<Titular> {
  return pedir<Titular>(`/api/titulares/${encodeURIComponent(id)}`, { method: 'PUT', cuerpo: datos })
}

/**
 * `PUT /api/titulares/{id}/validar` (DEC-98): no se deshace, y desde ahí el huésped no cambia su nombre desde su cuenta.
 *
 * @throws ErrorApi 409 con `motivo` `IDENTIDAD_VALIDADA` si ya estaba validada (DEC-94)
 */
export function validarTitular(id: string): Promise<Titular> {
  return pedir<Titular>(`/api/titulares/${encodeURIComponent(id)}/validar`, { method: 'PUT' })
}
