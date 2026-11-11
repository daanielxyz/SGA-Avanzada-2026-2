// Cuentas del bloque Pago. El anticipo lo manda la API (DEC-95); lo que no separa son los cargos del saldo, así que esos
// se calculan aquí para mostrarlos.
import type { Alojamiento } from '../api/alojamiento'
import type { Cargo, Folio } from '../api/folio'
import type { EstadoReserva, Reserva } from '../api/reservas'
import { esConsumo, valorConSigno } from '../folio/movimientos'

const TERMINALES: readonly EstadoReserva[] = ['FINALIZADA', 'CANCELADA', 'NO_SHOW']

export function estaTerminada(estado: EstadoReserva): boolean {
  return TERMINALES.includes(estado)
}

/** Lo que falta pagar para cubrir el anticipo que manda la API; cero si ya está cubierto o no se exige. */
export function faltaParaAnticipo(reserva: Reserva, folio: Folio): number {
  return Math.max(0, (reserva.anticipoExigido ?? 0) - folio.totalPagado)
}

function sumaDe(cargos: Cargo[], tipo: string): number {
  return cargos.filter((cargo) => cargo.tipo === tipo).reduce((suma, cargo) => suma + cargo.valor, 0)
}

/** Lo retenido al cancelar o por el no-show (CAR-02). */
export function valorRetenido(folio: Folio): number {
  return sumaDe(folio.cargos, 'PENALIDAD_CANCELACION')
}

/**
 * Los consumos cargados al folio (minibar, lavandería…), netos de sus correcciones. Los extras de la reserva quedan
 * fuera: la reserva ya dice cuánto suman.
 */
export function otrosCargos(folio: Folio): number {
  const consumos = folio.cargos.filter(esConsumo)
  const ids = new Set(consumos.map((cargo) => cargo.id))
  const corregidos = folio.cargos
    .filter((cargo) => cargo.corrigeA && ids.has(cargo.corrigeA))
    .reduce((suma, cargo) => suma + valorConSigno(cargo), 0)
  return consumos.reduce((suma, cargo) => suma + cargo.valor, 0) + corregidos
}

/** Una reserva terminada solo recibe pagos si todavía debe algo; lo que tenga a favor se devuelve (PAG-06). */
export function puedeRegistrarPago(estado: EstadoReserva, folio: Folio): boolean {
  if (folio.cerrado) return false
  return !estaTerminada(estado) || folio.situacionSaldo === 'PENDIENTE'
}

/** Medios para recepción: el pago en línea es solo del portal (DEC-70). */
export function mediosDeRecepcion(alojamiento: Alojamiento): string[] {
  return alojamiento.mediosPago.filter((medio) => medio !== 'PAGO_EN_LINEA')
}

/** Cuánto se propone pagar: lo que falta del anticipo y, si ya está cubierto, lo que queda por pagar. */
export function montoSugerido(reserva: Reserva, folio: Folio): number {
  const falta = faltaParaAnticipo(reserva, folio)
  if (falta > 0) return falta
  return folio.situacionSaldo === 'PENDIENTE' ? folio.saldo : 0
}
