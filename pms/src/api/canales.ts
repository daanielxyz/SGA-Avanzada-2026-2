// Canales de venta, sus conflictos y la bitácora de cada canal externo: solo administración (CU-54 · CU-45 · CONF-04 ·
// DEC-62 · DEC-73 · DEC-129).
import { pedir } from './cliente'
import type { Pagina } from './reservas'

export type TipoCanal = 'PORTAL' | 'DIRECTO' | 'EXTERNO'

/** `CanalResponse`. */
export interface Canal {
  id: string
  nombre: string
  tipo: TipoCanal
  /** El nombre con el que quedó guardada la clave en el servidor, nunca la clave; solo en los externos. */
  referenciaCredencial: string | null
  activo: boolean
}

/** `ConflictoResponse`: una reserva externa que se rechazó para no vender dos veces la misma noche. */
export interface Conflicto {
  id: string
  canalId: string
  idExterno: string
  apartamento: string
  entrada: string
  /** Exclusiva (EST-01): esa noche no se cuenta. */
  salida: string
  detectadoEn: string
  motivo: string
  /** A quién llamar (DEC-129); los tres en null en los conflictos de antes. */
  titularNombre: string | null
  titularTelefono: string | null
  titularCorreo: string | null
  estado: 'PENDIENTE' | 'RESUELTO'
  resueltoPor: string | null
  resueltoEn: string | null
  decision: string | null
}

export type OperacionCanal = 'CONSULTAR_DISPONIBILIDAD' | 'CREAR_RESERVA' | 'CANCELAR_RESERVA' | 'PUBLICAR_DISPONIBILIDAD'
export type ResultadoEvento = 'EXITOSO' | 'DUPLICADO' | 'CONFLICTO' | 'RECHAZADO' | 'ERROR'

/** `EventoCanalResponse`: un mensaje con el canal, también los rechazados (BIT-01 · BIT-03). */
export interface EventoCanal {
  id: string
  operacion: OperacionCanal
  sentido: 'ENTRANTE' | 'SALIENTE'
  fechaHora: string
  /** Lo que mandó el canal, sin credenciales (BIT-04), pero con datos del huésped. */
  cargaUtil: string
  resultado: ResultadoEvento
  /** El código de la reserva o el motivo del rechazo. */
  detalle: string | null
}

/** `RegistrarCanalRequest`. */
export interface NuevoCanal {
  nombre: string
  tipo: TipoCanal
  referenciaCredencial?: string
}

/** `GET /api/canales?pagina=`: de 10 en 10. */
export function listarCanales(pagina: number): Promise<Pagina<Canal>> {
  return pedir(`/api/canales?pagina=${pagina}`)
}

/** `POST /api/canales`: nace activo. Que solo el externo traiga la referencia lo decide el dominio (409). */
export function registrarCanal(nuevo: NuevoCanal): Promise<Canal> {
  return pedir('/api/canales', { method: 'POST', cuerpo: nuevo })
}

/** `PUT /api/canales/{id}/desactivar`: deja de crear reservas nuevas; 409 si ya estaba inactivo. */
export function desactivarCanal(id: string): Promise<Canal> {
  return pedir(`/api/canales/${encodeURIComponent(id)}/desactivar`, { method: 'PUT' })
}

/** `GET /api/canales/{id}/eventos`: entre dos días incluidos, del más reciente al más antiguo; 404 si no existe. */
export function consultarBitacora(id: string, desde: string, hasta: string, pagina: number): Promise<Pagina<EventoCanal>> {
  const consulta = new URLSearchParams({ desde, hasta, pagina: String(pagina) })
  return pedir(`/api/canales/${encodeURIComponent(id)}/eventos?${consulta}`)
}

/** `GET /api/conflictos/pendientes?pagina=`: los que esperan revisión, de 10 en 10. */
export function listarConflictosPendientes(pagina: number): Promise<Pagina<Conflicto>> {
  return pedir(`/api/conflictos/pendientes?pagina=${pagina}`)
}

/** `PUT /api/conflictos/{id}/resolver`: no cambia ninguna reserva (CONF-02); 409 si ya estaba resuelto. */
export function resolverConflicto(id: string, decision: string): Promise<Conflicto> {
  return pedir(`/api/conflictos/${encodeURIComponent(id)}/resolver`, { method: 'PUT', cuerpo: { decision } })
}
