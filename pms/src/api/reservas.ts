// Reservas que consulta recepción (CU-25 · CU-49 · DEC-72).
import { pedir } from './cliente'

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
  vehiculos: number
  mascotas: number
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
