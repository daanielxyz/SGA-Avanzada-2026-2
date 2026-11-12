// Llamadas de reservar y pagar en el portal. Cada interface es el gemelo de un Request o Response del backend (el
// contrato está en docs/api/openapi.yaml). No hace falta cuenta: la reserva se la crea (DEC-68).
import type { Estadia, Precio } from './apartamentos'
import { pedir } from './cliente'

export type TipoDocumento = 'CC' | 'CE' | 'PASAPORTE' | 'TI'

/** Nombres aparte de los apellidos (BL-07 · DEC-98). La web envía los apellidos juntos en `primerApellido`. */
export interface NombreSeparado {
  nombres: string
  /** Vacío en las cuentas de antes de DEC-98, que guardan el nombre completo en nombres. */
  primerApellido?: string
  segundoApellido?: string
}

export interface SolicitudReserva {
  apartamento: string
  entrada: string
  salida: string
  /** Quien reserva y tiene la cuenta; puede no hospedarse (DEC-97): la web pregunta «Yo también me hospedo». */
  titular: NombreSeparado & {
    tipoDocumento: TipoDocumento
    numeroDocumento: string
    correo: string
    telefono: string
  }
  /** Quienes se alojan; el backend toma como huésped principal al primer mayor de edad (DEC-97). */
  ocupantes: (NombreSeparado & {
    fechaNacimiento: string
    tipoDocumento?: TipoDocumento
    numeroDocumento?: string
  })[]
  carros: number
  motos: number
  mascotas: number
}

/** Lo que suman parqueadero y mascotas a la estadía (DEC-89). */
export interface Extras {
  parqueadero: number
  mascotas: number
  total: number
}

export const SIN_EXTRAS: Extras = { parqueadero: 0, mascotas: 0, total: 0 }

/** `ReservaPortalResponse`: la reserva apartada y lo que falta para confirmarla (DEC-69). */
export interface ReservaApartada {
  reserva: { codigo: string; valor: Precio; avisoCuenta: string | null; extras: Extras }
  montoAPagar: number
  /** Hora de Colombia; si no se paga antes, la reserva vence. */
  pagarAntesDe: string
  /**
   * Solo llega al reservar y solo la tiene esta pestaña: con ella se paga o se libera la reserva sin cuenta. Vacía al
   * retomar el pago desde «mis reservas», donde vale la sesión (DEC-93).
   */
  clave?: string
}

/** `PagoPortalResponse`. */
export interface PagoAprobado {
  codigo: string
  estado: string
  apartamento: string
  entrada: string
  salida: string
  total: number
}

export interface Penalizacion {
  base: 'VALOR_TOTAL' | 'PAGADO' | 'ANTICIPO_EXIGIDO'
  porcentaje: number | null
  montoFijo: number | null
}

/** `PoliticaPortalResponse`: las condiciones que el huésped acepta al reservar (DEC-41). */
export interface PoliticaCancelacion {
  tramos: { antelacionMinHoras: number; penalizacion: Penalizacion }[]
  penalizacionNoShow: Penalizacion
}

/** La edad de cada persona a la llegada decide quién paga (RN-06). */
export function cotizar(apartamento: string, { entrada, salida }: Estadia, fechasNacimiento: string[]) {
  return pedir<Precio>('/api/cotizaciones', {
    method: 'POST',
    cuerpo: { apartamento, entrada, salida, fechasNacimiento },
  })
}

export function reservar(solicitud: SolicitudReserva) {
  return pedir<ReservaApartada>('/api/portal/reservas', { method: 'POST', cuerpo: solicitud })
}

// DEC-93: la clave va en una cabecera, nunca en la dirección, para que no quede en el historial ni en registros
const CABECERA_CLAVE = 'X-Reserva-Clave'

/** Con la clave de la reserva recién hecha o, sin ella, con la sesión del huésped. */
export function pagar(codigo: string, tokenPago: string, clave?: string) {
  return pedir<PagoAprobado>(`/api/portal/reservas/${encodeURIComponent(codigo)}/pago`, {
    method: 'POST',
    cuerpo: { tokenPago },
    ...(clave ? { headers: { [CABECERA_CLAVE]: clave } } : { conSesion: true }),
  })
}

/** Suelta una reserva sin pagar: sus noches quedan libres de inmediato y no se cobra nada (DEC-93). */
export function liberar(codigo: string, clave: string) {
  return pedir<void>(`/api/portal/reservas/${encodeURIComponent(codigo)}/liberar`, {
    method: 'PUT',
    headers: { [CABECERA_CLAVE]: clave },
  })
}

export function consultarPolitica() {
  return pedir<PoliticaCancelacion>('/api/portal/politica')
}
