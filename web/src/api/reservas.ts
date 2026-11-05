// Llamadas de reservar y pagar en el portal. Cada interface es el gemelo de un Request o Response del backend (el
// contrato está en docs/api/openapi.yaml). No hace falta cuenta: la reserva se la crea (DEC-68).
import type { Estadia, Precio } from './apartamentos'
import { pedir } from './cliente'

export type TipoDocumento = 'CC' | 'CE' | 'PASAPORTE' | 'TI'

export interface SolicitudReserva {
  apartamento: string
  entrada: string
  salida: string
  titular: {
    nombre: string
    tipoDocumento: TipoDocumento
    numeroDocumento: string
    correo: string
    telefono: string
  }
  /** El grupo completo, incluido el titular con su documento (TIT-01). */
  ocupantes: {
    nombre: string
    fechaNacimiento: string
    tipoDocumento?: TipoDocumento
    numeroDocumento?: string
  }[]
  vehiculos: number
  mascotas: number
}

/** `ReservaPortalResponse`: la reserva apartada y lo que falta para confirmarla (DEC-69). */
export interface ReservaApartada {
  reserva: { codigo: string; valor: Precio; avisoCuenta: string | null }
  montoAPagar: number
  /** Hora de Colombia; si no se paga antes, la reserva vence. */
  pagarAntesDe: string
  /** Lo que se suma a la estancia y se paga al llegar: hoy, las mascotas (RP-03). */
  cargosAdicionales: number
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

export function pagar(codigo: string, tokenPago: string) {
  return pedir<PagoAprobado>(`/api/portal/reservas/${encodeURIComponent(codigo)}/pago`, {
    method: 'POST',
    cuerpo: { tokenPago },
  })
}

export function consultarPolitica() {
  return pedir<PoliticaCancelacion>('/api/portal/politica')
}
