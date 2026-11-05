// Llamadas del huésped: entrar con PIN, su cuenta y sus reservas (DEC-68 · DEC-69). Cada interface es el gemelo de un
// Request o Response del backend (el contrato está en docs/api/openapi.yaml).
import type { Pagina, Precio } from './apartamentos'
import { pedir } from './cliente'
import type { Extras, TipoDocumento } from './reservas'

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
  nombre: string
  tipoDocumento: TipoDocumento
  numeroDocumento: string
  correo: string | null
  telefono: string | null
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

export function listarMisReservas(pagina = 0) {
  return pedir<Pagina<ReservaDelHuesped>>(`/api/mis-reservas?pagina=${pagina}`, { conSesion: true })
}

export function consultarReserva(codigo: string) {
  return pedir<ReservaDelHuesped>(`/api/reservas/${encodeURIComponent(codigo)}`, { conSesion: true })
}

export function consultarCuentaDeReserva(codigo: string) {
  return pedir<CuentaDeReserva>(`/api/reservas/${encodeURIComponent(codigo)}/folio`, { conSesion: true })
}

export function cancelarReserva(codigo: string) {
  return pedir<Cancelacion>(`/api/reservas/${encodeURIComponent(codigo)}/cancelar`, {
    method: 'PUT',
    conSesion: true,
  })
}
