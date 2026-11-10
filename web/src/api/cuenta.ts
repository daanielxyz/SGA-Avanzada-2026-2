// Llamadas del huésped: entrar con PIN, su cuenta y sus reservas (DEC-68 · DEC-69). Cada interface es el gemelo de un
// Request o Response del backend (el contrato está en docs/api/openapi.yaml).
import type { Pagina, Precio } from './apartamentos'
import { pedir } from './cliente'
import type { Extras, PoliticaCancelacion, TipoDocumento } from './reservas'

export interface CredencialesHuesped {
  correo: string
  numeroDocumento: string
}

/** `SesionResponse`. */
export interface SesionIniciada {
  token: string
  expiraEn: string
  nombre: string
  rol: string
}

/** `CuentaResponse`: los datos del titular. */
export interface Cuenta {
  id: string
  /** Nombres y apellidos juntos. */
  nombre: string
  /** En las cuentas de antes de DEC-98 trae el nombre completo y los apellidos llegan vacíos. */
  nombres: string
  primerApellido: string | null
  segundoApellido: string | null
  tipoDocumento: TipoDocumento
  numeroDocumento: string
  correo: string | null
  telefono: string | null
  /** El hotel ya validó su identidad en persona: el nombre solo lo cambia recepción (DEC-98). */
  validado: boolean
}

/** `ActualizarCuentaRequest`: con la identidad validada, un nombre distinto responde 409 (DEC-98). */
export interface CambioCuenta {
  nombres: string
  /** Los apellidos juntos, como los escribe en «Apellidos». */
  primerApellido?: string
  segundoApellido?: string
  telefono: string
}

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA' | 'NO_SHOW'

/** De `ReservaResponse`, lo que ve el huésped. */
export interface ReservaDelHuesped {
  codigo: string
  apartamento: string
  entrada: string
  salida: string
  estado: EstadoReserva
  ocupantes: { id: string; nombre: string }[]
  valor: Precio
  extras: Extras
  carros: number
  motos: number
  mascotas: number
}

/** Cómo se le nombra cada estado al huésped. */
export const ESTADOS: Record<EstadoReserva, string> = {
  PENDIENTE: 'Falta pagar',
  CONFIRMADA: 'Confirmada',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  NO_SHOW: 'No hubo llegada',
}

export type SituacionSaldo = 'PENDIENTE' | 'A_FAVOR' | 'AL_DIA'

/** De `FolioResponse`: cuánto lleva pagado y cómo queda la cuenta. */
export interface CuentaDeReserva {
  totalPagado: number
  saldo: number
  situacionSaldo: SituacionSaldo
}

/** `CancelacionResponse`: lo retenido según las condiciones que aceptó y cómo queda la cuenta. */
export interface Cancelacion {
  codigo: string
  estado: EstadoReserva
  retenido: number
  saldo: number
  situacionSaldo: SituacionSaldo
}

/** La respuesta es la misma exista o no la cuenta: así nadie averigua quién tiene una (DEC-69). */
export function solicitarPin(credenciales: CredencialesHuesped) {
  return pedir<{ mensaje: string }>('/api/auth/huesped/pin', { method: 'POST', cuerpo: credenciales })
}

export function iniciarSesion(credenciales: CredencialesHuesped, pin: string) {
  return pedir<SesionIniciada>('/api/auth/huesped/login', { method: 'POST', cuerpo: { ...credenciales, pin } })
}

export function cerrarSesion() {
  return pedir<void>('/api/auth/logout', { method: 'POST', conSesion: true })
}

export function consultarCuenta() {
  return pedir<Cuenta>('/api/mi-cuenta', { conSesion: true })
}

export function actualizarCuenta(cambio: CambioCuenta) {
  return pedir<Cuenta>('/api/mi-cuenta', { method: 'PUT', cuerpo: cambio, conSesion: true })
}

type Documento = Pick<Cuenta, 'tipoDocumento' | 'numeroDocumento'>

/**
 * Paso 1 del cambio de correo (DEC-69): el PIN llega al teléfono y al correo actual, no al nuevo, para que quien
 * tome el celular desbloqueado no se quede con la cuenta. La respuesta es la misma exista o no la cuenta.
 */
export function solicitarCambioCorreo({ tipoDocumento, numeroDocumento }: Documento, nuevoCorreo: string) {
  return pedir<{ mensaje: string }>('/api/auth/huesped/cambio-correo', {
    method: 'POST',
    cuerpo: { tipoDocumento, numeroDocumento, nuevoCorreo },
  })
}

/** Paso 2: con el PIN, el correo de la cuenta queda cambiado. */
export function confirmarCambioCorreo({ tipoDocumento, numeroDocumento }: Documento, pin: string) {
  return pedir<{ mensaje: string }>('/api/auth/huesped/cambio-correo', {
    method: 'PUT',
    cuerpo: { tipoDocumento, numeroDocumento, pin },
  })
}

export function listarMisReservas(pagina = 0) {
  return pedir<Pagina<ReservaDelHuesped>>(`/api/mis-reservas?pagina=${pagina}`, { conSesion: true })
}

export function consultarReserva(codigo: string) {
  return pedir<ReservaDelHuesped>(`/api/reservas/${encodeURIComponent(codigo)}`, { conSesion: true })
}

export function consultarCuentaDeReserva(codigo: string) {
  return pedir<CuentaDeReserva>(`/api/reservas/${encodeURIComponent(codigo)}/folio`, { conSesion: true })
}

/** `SimulacionCancelacionResponse`: lo que pasaría al cancelar ahora, sin cancelar (DEC-91). */
export interface SimulacionCancelacion {
  retenido: number
  saldo: number
  situacionSaldo: SituacionSaldo
}

export function simularCancelacion(codigo: string) {
  return pedir<SimulacionCancelacion>(`/api/reservas/${encodeURIComponent(codigo)}/cancelacion`, { conSesion: true })
}

/** `PagoPendienteResponse`: lo que falta para confirmar en línea una reserva pendiente (DEC-91). */
export interface PagoPendiente {
  montoAPagar: number
  /** Hora de Colombia; si no se paga antes, la reserva vence. */
  pagarAntesDe: string
  /** La política congelada en la reserva: la que se le aplica, no la vigente (RN-13). */
  condiciones: PoliticaCancelacion
}

export function consultarPagoPendiente(codigo: string) {
  return pedir<PagoPendiente>(`/api/reservas/${encodeURIComponent(codigo)}/pago-en-linea`, { conSesion: true })
}

export function cancelarReserva(codigo: string) {
  return pedir<Cancelacion>(`/api/reservas/${encodeURIComponent(codigo)}/cancelar`, {
    method: 'PUT',
    conSesion: true,
  })
}
