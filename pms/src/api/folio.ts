// Folio de una reserva: en el sprint 2 solo se consulta y se le registran pagos; el resto llega en el sprint 3.
import { pedir } from './cliente'

export type SituacionSaldo = 'PENDIENTE' | 'A_FAVOR' | 'AL_DIA'

export interface Cargo {
  id: string
  tipo: string
  concepto: string
  valor: number
  sentido: string | null
  fecha: string
}

/** Lo que usa el PMS de `FolioResponse`. El saldo llega positivo: quién le debe a quién lo dice `situacionSaldo`. */
export interface Folio {
  id: string
  reserva: string
  cargos: Cargo[]
  totalPagado: number
  saldo: number
  situacionSaldo: SituacionSaldo
  cerrado: boolean
}

export interface NuevoPago {
  medio: string
  monto: number
  fecha: string
}

/** `GET /api/reservas/{codigo}/folio`. */
export function consultarFolio(codigo: string): Promise<Folio> {
  return pedir<Folio>(`/api/reservas/${encodeURIComponent(codigo)}/folio`)
}

/**
 * `POST /api/reservas/{codigo}/folio/pagos`.
 *
 * @throws ErrorApi 400 si un campo no tiene el formato esperado; 409 si el folio está cerrado, la fecha es futura o
 *                  el medio no está habilitado
 */
export function registrarPago(codigo: string, pago: NuevoPago): Promise<Folio> {
  return pedir<Folio>(`/api/reservas/${encodeURIComponent(codigo)}/folio/pagos`, { method: 'POST', cuerpo: pago })
}
