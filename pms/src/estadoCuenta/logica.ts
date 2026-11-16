// Lo que dice el estado de cuenta de una reserva (DEC-133): cada cargo y cada pago, con el IVA aparte, y los totales.
// Sale del folio tal como lo manda la API; no calcula nada que el backend no haya cargado.
import type { Cargo, Folio, SituacionSaldo } from '../api/folio'
import { conceptoVisible, filasDeCargos, filasDePagos, montoConSigno, tipoDelPago, valorConSigno } from '../folio/movimientos'
import { nombreLegible } from '../formato'

export interface LineaCargo {
  id: string
  fecha: string
  concepto: string
  /** Con su signo: una corrección que disminuye es negativa. */
  valor: number
  /** El IVA de la estancia o su corrección. */
  iva: boolean
  /** Una corrección de otro cargo, que va justo debajo de él. */
  correccion: boolean
}

export interface LineaPago {
  id: string
  fecha: string
  /** «Abono · Efectivo», «Devolución · Transferencia». */
  concepto: string
  /** Con su signo: un reverso o una devolución restan. */
  valor: number
}

/** El IVA de la estancia (DEC-133), o la corrección de uno: va aparte del resto de los cargos. */
export function esDelIva(cargo: Cargo, cargos: Cargo[]): boolean {
  if (cargo.tipo === 'IMPUESTO') return true
  return cargo.tipo === 'AJUSTE' && cargos.some((otro) => otro.id === cargo.corrigeA && otro.tipo === 'IMPUESTO')
}

/** Los cargos en el orden del folio, cada uno con su corrección debajo. */
export function lineasDeCargos(folio: Folio): LineaCargo[] {
  const linea = (cargo: Cargo, correccion: boolean): LineaCargo => ({
    id: cargo.id,
    fecha: cargo.fecha,
    concepto: conceptoVisible(cargo, folio.cargos),
    valor: valorConSigno(cargo),
    iva: esDelIva(cargo, folio.cargos),
    correccion,
  })
  return filasDeCargos(folio.cargos).flatMap(({ movimiento, reverso }) =>
    reverso ? [linea(movimiento, false), linea(reverso, true)] : [linea(movimiento, false)],
  )
}

/** Los pagos en el orden del folio, cada abono revertido con su reverso debajo. */
export function lineasDePagos(folio: Folio): LineaPago[] {
  const linea = (pago: NonNullable<Folio['pagos']>[number]): LineaPago => ({
    id: pago.id,
    fecha: pago.fecha,
    concepto: `${tipoDelPago(pago)} · ${nombreLegible(pago.medio)}`,
    valor: montoConSigno(pago),
  })
  return filasDePagos(folio.pagos ?? []).flatMap(({ movimiento, reverso }) =>
    reverso ? [linea(movimiento), linea(reverso)] : [linea(movimiento)],
  )
}

export interface Totales {
  /** Los cargos sin el IVA. */
  subtotal: number
  /** El IVA neto: lo cargado menos lo corregido. */
  iva: number
  /** Subtotal más IVA: lo mismo que suman los cargos del folio. */
  total: number
  /** Si el folio tiene o tuvo IVA: sin él, el documento no muestra la línea. */
  conIva: boolean
}

export function totales(folio: Folio): Totales {
  let subtotal = 0
  let iva = 0
  for (const cargo of folio.cargos) {
    if (esDelIva(cargo, folio.cargos)) iva += valorConSigno(cargo)
    else subtotal += valorConSigno(cargo)
  }
  return { subtotal, iva, total: subtotal + iva, conIva: folio.cargos.some((c) => esDelIva(c, folio.cargos)) }
}

/** El saldo llega positivo: quién le debe a quién lo dice la situación. */
export const TEXTO_SALDO: Record<SituacionSaldo, string> = {
  PENDIENTE: 'Saldo pendiente',
  A_FAVOR: 'Saldo a favor del huésped',
  AL_DIA: 'Sin saldo pendiente',
}

/** «C.C. 1094000», «Pasaporte AB123»: la cédula y las demás, en las siglas que lee el huésped. */
export function documentoLegible(tipo: string | null | undefined, numero: string | null | undefined): string | null {
  if (!numero) return null
  const siglas: Record<string, string> = { CC: 'C.C.', CE: 'C.E.', TI: 'T.I.', PASAPORTE: 'Pasaporte' }
  return `${tipo ? (siglas[tipo] ?? tipo) : ''} ${numero}`.trim()
}

/** «dirección · ciudad», sin los que falten. */
export function lugarDelAlojamiento(direccion?: string | null, ciudad?: string | null): string {
  return [direccion, ciudad].filter((parte) => parte && parte.trim()).join(' · ')
}
