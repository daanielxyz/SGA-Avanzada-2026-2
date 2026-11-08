// Reservas que consulta recepción (CU-25 · CU-49 · DEC-72).
import { pedir } from './cliente'
import type { SituacionSaldo } from './folio'

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_SHOW'
export type CanalOrigen = 'PORTAL' | 'DIRECTO' | 'EXTERNO'

/** Cómo se le muestra cada estado a la persona. */
export const NOMBRE_DEL_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_SHOW: 'No se presentó',
}

export const NOMBRE_DEL_CANAL: Record<CanalOrigen, string> = {
  PORTAL: 'Portal',
  DIRECTO: 'Directo',
  EXTERNO: 'Externo',
}

export interface Ocupante {
  id: string
  nombre: string
  fechaNacimiento: string | null
  tipoDocumento: string | null
  numeroDocumento: string | null
  nacionalidad: string | null
}

export interface Noche {
  noche: string
  temporada: string
  tarifa: number
  ocupantesFacturables: number
  subtotal: number
}

/** `ReservaResponse` del backend. */
export interface Reserva {
  codigo: string
  apartamento: string
  titularId: string
  titularNombre: string | null
  entrada: string
  salida: string
  estado: EstadoReserva
  canalOrigen: CanalOrigen
  ocupantes: Ocupante[]
  horaEstimadaLlegada: string | null
  valor: { desglose: Noche[]; total: number }
  politicaId: string
  creadaEn: string
  /** Carros más motos (obsoleto: se usan `carros` y `motos`). */
  vehiculos: number
  carros: number
  motos: number
  mascotas: number
  /** Lo que suman parqueadero y mascotas, congelado al crear y aparte de `valor` (DEC-89). */
  extras: { parqueadero: number; mascotas: number; total: number }
  /** Solo llega al crear: el titular ya tenía una cuenta con otro correo. */
  avisoCuenta?: string | null
}

/** `PaginaResponse` del backend: `numero` empieza en 0 y las páginas son de 10 (DEC-18). */
export interface Pagina<T> {
  contenido: T[]
  numero: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

/**
 * `GET /api/reservas`.
 *
 * @param consulta los filtros ya escritos como parámetros: `desde`, `hasta`, `estado`, `apartamento`, `canal`,
 *                 `titular` y `pagina`
 * @throws ErrorApi 409 si `desde` no es anterior a `hasta`, 400 si un filtro no tiene el formato esperado
 */
export function listarReservas(consulta: string): Promise<Pagina<Reserva>> {
  return pedir<Pagina<Reserva>>(`/api/reservas${consulta ? `?${consulta}` : ''}`)
}

/**
 * `GET /api/reservas/{codigo}`.
 *
 * @throws ErrorApi 404 `NO_ENCONTRADO` si no existe
 */
export function consultarReserva(codigo: string): Promise<Reserva> {
  return pedir<Reserva>(`/api/reservas/${encodeURIComponent(codigo)}`)
}

/** `CancelacionResponse` del backend: lo que queda en el folio después de cancelar o declarar el no-show. */
export interface Liquidacion {
  codigo: string
  estado: EstadoReserva
  retenido: number
  saldo: number
  situacionSaldo: SituacionSaldo
}

function accion<T>(codigo: string, nombre: string, cuerpo?: unknown): Promise<T> {
  return pedir<T>(`/api/reservas/${encodeURIComponent(codigo)}/${nombre}`, { method: 'PUT', cuerpo })
}

/**
 * `PUT /api/reservas/{codigo}/hora-llegada`.
 *
 * @param hora HH:mm
 * @throws ErrorApi 409 si la reserva ya inició o terminó
 */
export function indicarHoraLlegada(codigo: string, hora: string): Promise<Reserva> {
  return accion(codigo, 'hora-llegada', { hora })
}

/**
 * `PUT /api/reservas/{codigo}/confirmar` (RN-08 · RN-09 · RES-15).
 *
 * @throws ErrorApi 409 si no está pendiente, le falta la hora de llegada o lo pagado no cubre el anticipo
 */
export function confirmarReserva(codigo: string): Promise<Reserva> {
  return accion(codigo, 'confirmar')
}

/**
 * `PUT /api/reservas/{codigo}/cancelar` (RN-08 · RN-13).
 *
 * @throws ErrorApi 409 si no está pendiente ni confirmada
 */
export function cancelarReserva(codigo: string): Promise<Liquidacion> {
  return accion(codigo, 'cancelar')
}

/**
 * `PUT /api/reservas/{codigo}/no-show` (RN-08 · RES-16).
 *
 * @throws ErrorApi 409 si no está confirmada o aún no llega la hora límite
 */
export function declararNoShow(codigo: string): Promise<Liquidacion> {
  return accion(codigo, 'no-show')
}

/** `CotizacionResponse`: el valor de una estancia, antes de crear la reserva. */
export interface Cotizacion {
  desglose: Noche[]
  total: number
}

/**
 * `POST /api/cotizaciones` (público).
 *
 * @throws ErrorApi 409 si falta una tarifa o la estancia es inválida
 */
export function cotizarEstancia(datos: {
  apartamento: string
  entrada: string
  salida: string
  fechasNacimiento: string[]
}): Promise<Cotizacion> {
  return pedir<Cotizacion>('/api/cotizaciones', { method: 'POST', cuerpo: datos })
}

export interface NuevaReserva {
  apartamento: string
  entrada: string
  salida: string
  canalOrigen: 'DIRECTO'
  titular: { nombre: string; tipoDocumento: string; numeroDocumento: string; correo: string; telefono: string }
  ocupantes: {
    nombre: string
    fechaNacimiento: string
    tipoDocumento?: string
    numeroDocumento?: string
    nacionalidad?: string
  }[]
  horaEstimadaLlegada?: string
  carros: number
  motos: number
  mascotas: number
}

/**
 * `POST /api/reservas` (CU-11).
 *
 * @throws ErrorApi 400 si un campo no tiene el formato esperado; 409 si el apartamento ya no está disponible o se
 *                  viola una regla de creación (RN-01 · RN-02 · RN-03 · RP-01 · RP-02 · RP-03 · TIT-01)
 */
export function crearReserva(datos: NuevaReserva): Promise<Reserva> {
  return pedir<Reserva>('/api/reservas', { method: 'POST', cuerpo: datos })
}
