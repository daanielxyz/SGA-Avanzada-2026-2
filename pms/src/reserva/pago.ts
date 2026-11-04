// Cuentas del bloque Pago. La API no trae el monto del anticipo ni separa los cargos en el saldo, así que se calculan
// aquí para mostrarlos; quien decide al confirmar sigue siendo el backend (RES-15).
import type { Alojamiento } from '../api/alojamiento'
import type { Cargo, Folio } from '../api/folio'
import type { EstadoReserva, Reserva } from '../api/reservas'

const TERMINALES: readonly EstadoReserva[] = ['FINALIZADA', 'CANCELADA', 'NO_SHOW']

export function estaTerminada(estado: EstadoReserva): boolean {
  return TERMINALES.includes(estado)
}

/** El anticipo se calcula sobre el valor congelado, sin las mascotas ni otros cargos, y se redondea al peso (RES-15). */
export function anticipoExigido(total: number, pct: number): number {
  return Math.round((total * pct) / 100)
}

/** Lo que falta pagar para cubrir el anticipo; cero si ya está cubierto o no se exige. */
export function faltaParaAnticipo(reserva: Reserva, folio: Folio, alojamiento: Alojamiento): number {
  const exigido = anticipoExigido(reserva.valor.total, alojamiento.parametros.anticipoPct)
  return Math.max(0, exigido - folio.totalPagado)
}

function sumaDe(cargos: Cargo[], tipo: string): number {
  return cargos.filter((cargo) => cargo.tipo === tipo).reduce((suma, cargo) => suma + cargo.valor, 0)
}

/** Lo retenido al cancelar o por el no-show (CAR-02). */
export function valorRetenido(folio: Folio): number {
  return sumaDe(folio.cargos, 'PENALIDAD_CANCELACION')
}

/** Lo que el saldo cobra aparte del alojamiento, como las mascotas (RP-03). */
export function otrosCargos(folio: Folio): number {
  return sumaDe(folio.cargos, 'SERVICIO_ADICIONAL')
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
export function montoSugerido(reserva: Reserva, folio: Folio, alojamiento: Alojamiento): number {
  const falta = faltaParaAnticipo(reserva, folio, alojamiento)
  if (falta > 0) return falta
  return folio.situacionSaldo === 'PENDIENTE' ? folio.saldo : 0
}
