// Cuentas del bloque Pago. La API no trae el monto del anticipo ni separa los cargos en el saldo, así que se calculan
// aquí para mostrarlos; quien decide al confirmar sigue siendo el backend (RES-15).
import type { Alojamiento } from '../api/alojamiento'
import type { Cargo, Folio } from '../api/folio'
import type { EstadoReserva, Reserva } from '../api/reservas'

const TERMINALES: readonly EstadoReserva[] = ['FINALIZADA', 'CANCELADA', 'NO_SHOW']

export function estaTerminada(estado: EstadoReserva): boolean {
  return TERMINALES.includes(estado)
}

/** La estancia más sus extras (parqueadero y mascotas), congelados al crear: la base del anticipo (DEC-89). */
export function valorConExtras(reserva: Reserva): number {
  return reserva.valor.total + reserva.extras.total
}

/** El anticipo se calcula sobre la estancia y sus extras, sin los consumos, y se redondea al peso (RES-15 · DEC-89). */
export function anticipoExigido(reserva: Reserva, pct: number): number {
  return Math.round((valorConExtras(reserva) * pct) / 100)
}

/** Lo que falta pagar para cubrir el anticipo; cero si ya está cubierto o no se exige. */
export function faltaParaAnticipo(reserva: Reserva, folio: Folio, alojamiento: Alojamiento): number {
  const exigido = anticipoExigido(reserva, alojamiento.parametros.anticipoPct)
  return Math.max(0, exigido - folio.totalPagado)
}

function sumaDe(cargos: Cargo[], tipo: string): number {
  return cargos.filter((cargo) => cargo.tipo === tipo).reduce((suma, cargo) => suma + cargo.valor, 0)
}

/** Lo retenido al cancelar o por el no-show (CAR-02). */
export function valorRetenido(folio: Folio): number {
  return sumaDe(folio.cargos, 'PENALIDAD_CANCELACION')
}

// El backend carga los extras de la reserva al folio como servicios con estos conceptos (`Folio.cargarExtras`); las
// reservas anteriores a DEC-89 traen «Mascotas (n)». La reserva ya dice cuánto suman, así que aquí se dejan fuera.
function esExtraDeLaReserva(cargo: Cargo): boolean {
  return cargo.concepto === 'Parqueadero' || cargo.concepto === 'Mascotas' || cargo.concepto.startsWith('Mascotas (')
}

/** Los consumos cargados al folio (minibar, lavandería…), sin los extras de la reserva. */
export function otrosCargos(folio: Folio): number {
  return folio.cargos
    .filter((cargo) => cargo.tipo === 'SERVICIO_ADICIONAL' && !esExtraDeLaReserva(cargo))
    .reduce((suma, cargo) => suma + cargo.valor, 0)
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
