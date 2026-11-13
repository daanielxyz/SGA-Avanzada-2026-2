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

export type Genero = 'FEMENINO' | 'MASCULINO' | 'OTRO'

export type MotivoViaje =
  | 'TURISMO_VACACIONES'
  | 'EVENTOS'
  | 'ESTUDIOS'
  | 'TRATAMIENTO_MEDICO'
  | 'ASUNTOS_JURIDICOS'
  | 'NEGOCIOS'
  | 'TRANSITO'
  | 'COMPRAS'
  | 'RELIGION'
  | 'VISITA_FAMILIARES_AMIGOS'

export interface Ocupante {
  id: string
  /** Nombres y apellidos juntos. */
  nombre: string
  fechaNacimiento: string | null
  tipoDocumento: string | null
  numeroDocumento: string | null
  nacionalidad: string | null
  /** Los campos del TRA (BL-07). Pueden faltar en respuestas de antes; `nombre` siempre llega. */
  nombres?: string | null
  primerApellido?: string | null
  segundoApellido?: string | null
  genero?: Genero | string | null
  paisResidencia?: string | null
  ciudadResidencia?: string | null
  ciudadProcedencia?: string | null
  ciudadDestino?: string | null
  motivoViaje?: MotivoViaje | string | null
  /** Lo que le falta para el TRA, de DOCUMENTO a MOTIVO_VIAJE; vacío si está completo (DEC-96). */
  datosTraFaltantes?: string[]
  /** Huésped principal: se aloja, es mayor de edad y va de primero en el TRA (DEC-97). */
  principal?: boolean
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
  /** Lo que debe estar pagado para confirmarla (RES-15 · DEC-95). Solo llega en el detalle; en el listado es `null`. */
  anticipoExigido: number | null
  /** Quién declaró el no-show y cuándo (DEC-103). Solo en el detalle; nulo si no es un no-show o es anterior a V24. */
  noShow?: { autor: string; autorNombre: string | null; fechaHora: string } | null
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
 * Las reservas en curso cuya fecha de salida ya pasó sin registrarse. Mientras sigan en curso, su apartamento sigue
 * ocupado y no se entrega a la siguiente. Un alojamiento tiene pocas a la vez, así que basta la primera página.
 */
export async function listarSalidasVencidas(hoy: string, apartamento?: string): Promise<Reserva[]> {
  const consulta = new URLSearchParams({ estado: 'EN_CURSO', ...(apartamento ? { apartamento } : {}) })
  const pagina = await listarReservas(consulta.toString())
  return pagina.contenido.filter((reserva) => reserva.salida < hoy)
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

/** `SimulacionCancelacionResponse`: lo que pasaría al cancelar en este momento, sin cancelar (DEC-91). */
export interface SimulacionCancelacion {
  retenido: number
  saldo: number
  situacionSaldo: SituacionSaldo
}

/**
 * `GET /api/reservas/{codigo}/cancelacion` (RN-13 · DEC-91). El valor es el de este momento: si se cancela más tarde
 * puede caer en otro tramo de la política.
 *
 * @throws ErrorApi 409 si la reserva ya no se puede cancelar
 */
export function simularCancelacion(codigo: string): Promise<SimulacionCancelacion> {
  return pedir<SimulacionCancelacion>(`/api/reservas/${encodeURIComponent(codigo)}/cancelacion`)
}

/**
 * `GET /api/reservas/{codigo}/no-show` (RES-16 · DEC-102): lo que se retendría al declarar el no-show en este momento.
 *
 * @throws ErrorApi 409 si no está confirmada (`ESTADO_NO_PERMITIDO`) o aún no llega la hora límite (`ANTES_DE_TIEMPO`)
 */
export function simularNoShow(codigo: string): Promise<SimulacionCancelacion> {
  return pedir<SimulacionCancelacion>(`/api/reservas/${encodeURIComponent(codigo)}/no-show`)
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

/** Un registro de llegada: al corregirlo, el anterior queda anulado como historial (DEC-61). */
export interface RegistroLlegada {
  id: string
  fechaHora: string
  autor: string
  /** Nombre vigente de quien lo registró; nulo si ya no existe (DEC-114). */
  autorNombre?: string | null
  anulado: boolean
  /** Solo en el registro que corrige a uno anulado. */
  motivoCorreccion: string | null
  motivoSinPago: string | null
}

/** La salida registrada (CU-22). */
export interface SalidaRegistrada {
  id: string
  fechaHora: string
  autor: string
  autorNombre?: string | null
}

/** `EstanciaResponse` del backend: cómo quedaron la reserva, el apartamento y el folio. */
export interface Estancia {
  codigo: string
  estado: EstadoReserva
  apartamento: string
  estadoOperativo: string
  /** Puede faltar en respuestas viejas. */
  registros?: RegistroLlegada[]
  salida?: SalidaRegistrada | null
  folioCerrado: boolean
  saldo: number
  situacionSaldo: SituacionSaldo
}

/**
 * `GET /api/reservas/{codigo}/estancia` (DEC-114, solo recepción y administración): los registros de llegada, el vigente
 * y los anulados, y la salida si la hubo. Sin llegada responde con `registros` vacío, no 404.
 */
export function consultarEstancia(codigo: string): Promise<Estancia> {
  return pedir<Estancia>(`/api/reservas/${encodeURIComponent(codigo)}/estancia`)
}

/**
 * `PUT /api/reservas/{codigo}/registrar-llegada` (CU-21 · RN-10 · RN-11 · BL-22): la reserva pasa a en curso y el
 * apartamento a ocupado.
 *
 * @param motivoSinPago solo si la estancia y sus extras no están pagados; hasta 500 caracteres
 * @throws ErrorApi 409 si no está confirmada, aún no es la fecha de entrada (`ANTES_DE_TIEMPO`), el apartamento no está
 *                  preparado o falta pagar sin motivo (`ESTANCIA_SIN_PAGAR`)
 */
export function registrarLlegada(codigo: string, motivoSinPago?: string): Promise<Estancia> {
  return accion(codigo, 'registrar-llegada', motivoSinPago ? { motivoSinPago } : undefined)
}

/**
 * `PUT /api/reservas/{codigo}/corregir-registro` (solo el administrador): el registro actual queda anulado.
 *
 * @param fechaHora AAAA-MM-DDTHH:mm, entre la entrada y este momento
 * @throws ErrorApi 409 si no está en curso o la fecha queda fuera; 403 si no es el administrador
 */
export function corregirRegistro(codigo: string, fechaHora: string, motivo: string): Promise<Estancia> {
  return accion(codigo, 'corregir-registro', { fechaHora, motivo })
}

/** Cuerpo de `datos-tra`: lo que no se envía se conserva, así que se puede guardar a medias (BL-07). */
export interface DatosTra {
  nombres?: string
  primerApellido?: string
  segundoApellido?: string
  tipoDocumento?: string
  numeroDocumento?: string
  nacionalidad?: string
  genero?: string
  paisResidencia?: string
  ciudadResidencia?: string
  ciudadProcedencia?: string
  ciudadDestino?: string
  motivoViaje?: string
}

/**
 * `PUT /api/reservas/{codigo}/ocupantes/{id}/datos-tra` (BL-07).
 *
 * @throws ErrorApi 400 si un campo no tiene el formato esperado; 409 si el documento ya está en el grupo o ya estaba
 *                  registrado y se intenta cambiar
 */
export function completarDatosTra(codigo: string, ocupanteId: string, datos: DatosTra): Promise<Reserva> {
  return accion(codigo, `ocupantes/${encodeURIComponent(ocupanteId)}/datos-tra`, datos)
}

/**
 * `PUT /api/reservas/{codigo}/ocupantes/{id}/principal` (DEC-97).
 *
 * @throws ErrorApi 409 `HUESPED_PRINCIPAL_INVALIDO` si es menor de edad
 */
export function designarPrincipal(codigo: string, ocupanteId: string): Promise<Reserva> {
  return accion(codigo, `ocupantes/${encodeURIComponent(ocupanteId)}/principal`)
}

export interface NuevoTitular {
  nombres: string
  primerApellido: string
  segundoApellido?: string
  tipoDocumento: string
  numeroDocumento: string
  correo: string
  telefono: string
}

/**
 * `PUT /api/reservas/{codigo}/cambiar-titular` (TIT-01 · DEC-97). Si ya hay un titular con ese documento, se usa ese.
 *
 * @throws ErrorApi 400 si un campo no tiene el formato esperado; 409 si ya es el titular o la reserva terminó
 */
export function cambiarTitular(codigo: string, titular: NuevoTitular): Promise<Reserva> {
  return accion(codigo, 'cambiar-titular', titular)
}

/** Cuerpo de `modificar`: la reserva completa como queda, no solo lo que cambia (DEC-38). */
export interface ModificacionReserva {
  apartamento: string
  entrada: string
  salida: string
  ocupantes: {
    nombre?: string
    nombres?: string
    primerApellido?: string
    segundoApellido?: string
    fechaNacimiento: string
    tipoDocumento?: string
    numeroDocumento?: string
    nacionalidad?: string
    principal?: boolean
  }[]
  carros: number
  motos: number
  mascotas: number
}

/**
 * `PUT /api/reservas/{codigo}/modificar` (CU-17 · TAR-06 · BL-23): recalcula con las tarifas de hoy y deja la diferencia
 * como ajuste en el folio.
 *
 * @throws ErrorApi 409 si no está pendiente ni confirmada, la entrada ya pasó, no hay cupo o supera las mascotas
 */
export function modificarReserva(codigo: string, datos: ModificacionReserva): Promise<Reserva> {
  return accion(codigo, 'modificar', datos)
}

/**
 * `PUT /api/reservas/{codigo}/registrar-salida` (CU-22 · CU-38 · SAL-03): cierra el folio, finaliza la reserva y deja
 * el apartamento pendiente de preparación.
 *
 * @param motivoAutorizacion solo para salir con saldo; solo el administrador puede enviarlo
 * @throws ErrorApi 409 si no está en curso (`ESTADO_NO_PERMITIDO`), hay saldo sin motivo (`SALDO_PENDIENTE`) o faltan
 *                  datos del TRA (`DATOS_TRA_INCOMPLETOS`); 403 si recepción manda el motivo
 */
export function registrarSalida(codigo: string, motivoAutorizacion?: string): Promise<Estancia> {
  return accion(codigo, 'registrar-salida', motivoAutorizacion ? { motivoAutorizacion } : undefined)
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
