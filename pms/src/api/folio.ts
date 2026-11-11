// Folio de una reserva (FOL-02 · DEC-61): sus cargos y pagos solo se agregan; se corrigen con un movimiento inverso
// (RN-16). Toda acción responde el folio ya actualizado.
import { pedir } from './cliente'

export type SituacionSaldo = 'PENDIENTE' | 'A_FAVOR' | 'AL_DIA'
export type TipoCargo = 'HOSPEDAJE' | 'SERVICIO_ADICIONAL' | 'PENALIDAD_CANCELACION' | 'AJUSTE'

export interface Cargo {
  id: string
  tipo: TipoCargo | string
  concepto: string
  /** El total: en un consumo ya viene multiplicado por la cantidad (DEC-107). */
  valor: number
  /** Solo en un AJUSTE: AUMENTA o DISMINUYE. */
  sentido: 'AUMENTA' | 'DISMINUYE' | string | null
  fecha: string
  /** El cargo que este AJUSTE revierte. */
  corrigeA?: string | null
  /** Quien hizo la corrección; vacío en los demás cargos (DEC-100 · DEC-103). */
  autorNombre?: string | null
  /** Unidades de un consumo; 1 en los demás cargos (DEC-107). */
  cantidad?: number
}

export interface Pago {
  id: string
  medio: string
  monto: number
  tipo: 'ABONO' | 'REVERSO' | string
  fecha: string
  /** El abono que este REVERSO corrige; vacío en una devolución. */
  reversaDe?: string | null
  /** Quien revirtió o devolvió; vacío en un abono (DEC-100 · DEC-103). */
  autorNombre?: string | null
}

export interface Autorizacion {
  motivo: string
  fechaHora: string
  autorNombre?: string | null
}

/** Lo que usa el PMS de `FolioResponse`. El saldo llega positivo: quién le debe a quién lo dice `situacionSaldo`. */
export interface Folio {
  id: string
  reserva: string
  cargos: Cargo[]
  pagos?: Pago[]
  totalPagado: number
  saldo: number
  situacionSaldo: SituacionSaldo
  cerrado: boolean
  /** Solo si se cerró con saldo distinto de cero (RN-17). */
  autorizacion?: Autorizacion | null
}

export interface NuevoPago {
  medio: string
  monto: number
  fecha: string
}

function ruta(codigo: string, resto = ''): string {
  return `/api/reservas/${encodeURIComponent(codigo)}/folio${resto}`
}

/** `GET /api/reservas/{codigo}/folio`. */
export function consultarFolio(codigo: string): Promise<Folio> {
  return pedir<Folio>(ruta(codigo))
}

/**
 * `POST /api/reservas/{codigo}/folio/pagos`.
 *
 * @throws ErrorApi 400 si un campo no tiene el formato esperado; 409 si el folio está cerrado, la fecha es futura o
 *                  el medio no está habilitado
 */
export function registrarPago(codigo: string, pago: NuevoPago): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, '/pagos'), { method: 'POST', cuerpo: pago })
}

/**
 * `POST /api/reservas/{codigo}/folio/devoluciones` (PAG-06): el dinero ya se le entregó al huésped por fuera.
 *
 * @throws ErrorApi 409 si el folio está cerrado, la fecha es futura, el medio no está habilitado o supera lo abonado
 */
export function registrarDevolucion(codigo: string, devolucion: NuevoPago): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, '/devoluciones'), { method: 'POST', cuerpo: devolucion })
}

/**
 * `POST /api/reservas/{codigo}/folio/servicios` (CAR-05 · BL-12). El valor sale del catálogo.
 *
 * @throws ErrorApi 409 si la reserva no está en curso, el folio está cerrado o el servicio no se puede cargar
 */
export function cargarConsumo(codigo: string, servicioId: string, cantidad: number): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, '/servicios'), { method: 'POST', cuerpo: { servicioId, cantidad } })
}

/**
 * `POST /api/reservas/{codigo}/folio/cargos/{id}/revertir` (RN-16 · CAR-03).
 *
 * @throws ErrorApi 409 `YA_REVERTIDO` o `FOLIO_CERRADO`
 */
export function revertirCargo(codigo: string, cargoId: string, motivo: string): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, `/cargos/${encodeURIComponent(cargoId)}/revertir`), {
    method: 'POST',
    cuerpo: { motivo },
  })
}

/**
 * `POST /api/reservas/{codigo}/folio/pagos/{id}/revertir` (RN-16 · PAG-03). No recibe motivo.
 *
 * @throws ErrorApi 409 `YA_REVERTIDO`, `FOLIO_CERRADO` o si supera lo abonado neto
 */
export function revertirPago(codigo: string, pagoId: string): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, `/pagos/${encodeURIComponent(pagoId)}/revertir`), { method: 'POST' })
}

/**
 * `PUT /api/reservas/{codigo}/folio/cerrar` (CU-28 · RN-17). Sin motivo exige el saldo en cero; el motivo solo lo
 * puede enviar el administrador.
 *
 * @throws ErrorApi 409 `SALDO_PENDIENTE` o `FOLIO_CERRADO`; 403 si recepción manda el motivo
 */
export function cerrarFolio(codigo: string, motivoAutorizacion?: string): Promise<Folio> {
  return pedir<Folio>(ruta(codigo, '/cerrar'), {
    method: 'PUT',
    cuerpo: motivoAutorizacion ? { motivoAutorizacion } : undefined,
  })
}
