import { describe, expect, it } from 'vitest'
import type { Cargo, Folio, Pago } from '../api/folio'
import { documentoLegible, esDelIva, lineasDeCargos, lineasDePagos, lugarDelAlojamiento, totales } from './logica'

const HOSPEDAJE: Cargo = { id: 'CAR-1', tipo: 'HOSPEDAJE', concepto: 'Hospedaje', valor: 1000000, sentido: null, fecha: '2026-11-10' }
const IVA: Cargo = {
  id: 'CAR-2', tipo: 'IMPUESTO', concepto: 'IVA del alojamiento (19 %)', valor: 190000, sentido: null, fecha: '2026-11-10',
}
const AGUA: Cargo = {
  id: 'CAR-3', tipo: 'SERVICIO_ADICIONAL', concepto: 'Agua', valor: 10000, sentido: null, fecha: '2026-11-11', cantidad: 2,
}
const QUITA_IVA: Cargo = {
  id: 'CAR-4', tipo: 'AJUSTE', concepto: 'Corrección de CAR-2: llegó con pasaporte', valor: 190000, sentido: 'DISMINUYE',
  fecha: '2026-11-11', corrigeA: 'CAR-2',
}

function folio(cargos: Cargo[], pagos: Pago[] = []): Folio {
  return { id: 'FOL-1', reserva: 'RES-2026-00042', cargos, pagos, totalPagado: 0, saldo: 0, situacionSaldo: 'AL_DIA', cerrado: false }
}

describe('estado de cuenta (DEC-133)', () => {
  it('separa el IVA del resto: subtotal sin IVA, IVA y total', () => {
    expect(totales(folio([HOSPEDAJE, IVA, AGUA]))).toEqual({ subtotal: 1010000, iva: 190000, total: 1200000, conIva: true })
  })

  it('sin IVA en el folio no lo muestra y el total es el de los cargos', () => {
    expect(totales(folio([HOSPEDAJE, AGUA]))).toEqual({ subtotal: 1010000, iva: 0, total: 1010000, conIva: false })
  })

  it('la corrección del IVA cuenta como IVA y lo deja en cero', () => {
    const cargos = [HOSPEDAJE, IVA, QUITA_IVA]
    expect(esDelIva(QUITA_IVA, cargos)).toBe(true)
    expect(totales(folio(cargos))).toMatchObject({ subtotal: 1000000, iva: 0, total: 1000000, conIva: true })
  })

  it('un ajuste de otro cargo no es IVA', () => {
    const corrigeAgua: Cargo = { ...QUITA_IVA, id: 'CAR-5', corrigeA: 'CAR-3', concepto: 'Corrección de CAR-3: error' }
    expect(esDelIva(corrigeAgua, [AGUA, corrigeAgua])).toBe(false)
  })

  it('cada cargo con su signo, la cantidad en el concepto y la corrección debajo del corregido', () => {
    const lineas = lineasDeCargos(folio([HOSPEDAJE, IVA, AGUA, QUITA_IVA]))
    expect(lineas.map((l) => [l.concepto, l.valor, l.iva, l.correccion])).toEqual([
      ['Hospedaje', 1000000, false, false],
      ['IVA del alojamiento (19 %)', 190000, true, false],
      ['Corrección de IVA del alojamiento (19 %): llegó con pasaporte', -190000, true, true],
      ['Agua × 2', 10000, false, false],
    ])
  })

  it('los pagos dicen qué fueron y con qué medio; reversos y devoluciones restan', () => {
    const pagos: Pago[] = [
      { id: 'PAG-1', medio: 'EFECTIVO', monto: 300000, tipo: 'ABONO', fecha: '2026-11-09' },
      { id: 'PAG-2', medio: 'TARJETA_CREDITO', monto: 50000, tipo: 'ABONO', fecha: '2026-11-10' },
      { id: 'PAG-3', medio: 'TARJETA_CREDITO', monto: 50000, tipo: 'REVERSO', fecha: '2026-11-10', reversaDe: 'PAG-2' },
      { id: 'PAG-4', medio: 'TRANSFERENCIA', monto: 20000, tipo: 'REVERSO', fecha: '2026-11-12' },
    ]
    expect(lineasDePagos(folio([], pagos)).map((l) => [l.concepto, l.valor])).toEqual([
      ['Abono · Efectivo', 300000],
      ['Abono · Tarjeta credito', 50000],
      ['Reverso del abono · Tarjeta credito', -50000],
      ['Devolución · Transferencia', -20000],
    ])
  })

  it('el documento en las siglas que lee el huésped, y nada si no hay número', () => {
    expect(documentoLegible('CC', '1094000')).toBe('C.C. 1094000')
    expect(documentoLegible('PASAPORTE', 'AB123')).toBe('Pasaporte AB123')
    expect(documentoLegible('RC', '55')).toBe('RC 55')
    expect(documentoLegible('CC', null)).toBeNull()
  })

  it('el lugar del alojamiento sin las partes que falten', () => {
    expect(lugarDelAlojamiento('Vereda Palestina km 2', 'Salento')).toBe('Vereda Palestina km 2 · Salento')
    expect(lugarDelAlojamiento('  ', 'Salento')).toBe('Salento')
    expect(lugarDelAlojamiento(null, undefined)).toBe('')
  })
})
