import { describe, expect, it } from 'vitest'
import type { Cargo, Folio, Pago } from '../api/folio'
import { otrosCargos } from '../reserva/pago'
import {
  accionesDelCargo,
  accionesDelFolio,
  cargosNetos,
  conceptoVisible,
  filasDeCargos,
  filasDePagos,
  puedeRevertirPago,
  tipoDelCargo,
  tipoDelPago,
} from './movimientos'

function cargo(id: string, cambios: Partial<Cargo> = {}): Cargo {
  return { id, tipo: 'SERVICIO_ADICIONAL', concepto: 'Minibar', valor: 38000, sentido: null, fecha: '2026-11-12', ...cambios }
}

function pago(id: string, cambios: Partial<Pago> = {}): Pago {
  return { id, medio: 'EFECTIVO', monto: 200000, tipo: 'ABONO', fecha: '2026-11-11', ...cambios }
}

const CARGOS: Cargo[] = [
  cargo('CAR-1', { tipo: 'HOSPEDAJE', concepto: 'Hospedaje', valor: 1350000 }),
  cargo('CAR-2', { concepto: 'Parqueadero', valor: 60000 }),
  cargo('CAR-4', { concepto: 'Agua', valor: 10000, cantidad: 2 }),
  cargo('CAR-5', { concepto: 'Lavandería', valor: 45000 }),
  cargo('CAR-6', {
    tipo: 'AJUSTE', concepto: 'Corrección de CAR-5: se cargó a la habitación equivocada', valor: 45000,
    sentido: 'DISMINUYE', corrigeA: 'CAR-5', autorNombre: 'Andrea Ruiz',
  }),
  cargo('CAR-7'),
]

function folio(cambios: Partial<Folio> = {}): Folio {
  return {
    id: 'FOL-1', reserva: 'RES-2026-00042', cargos: CARGOS, pagos: [], totalPagado: 1100000, saldo: 403000,
    situacionSaldo: 'PENDIENTE', cerrado: false, ...cambios,
  }
}

describe('Movimientos del folio (RN-15 · RN-16)', () => {
  it('pone cada corrección debajo del cargo que corrige, sin repetirla', () => {
    const filas = filasDeCargos(CARGOS)
    expect(filas.map((fila) => fila.movimiento.id)).toEqual(['CAR-1', 'CAR-2', 'CAR-4', 'CAR-5', 'CAR-7'])
    expect(filas[3].reverso?.id).toBe('CAR-6')
  })

  it('pone cada reverso debajo de su abono y deja la devolución en su lugar', () => {
    const pagos = [
      pago('PAG-1'),
      pago('PAG-2'),
      pago('PAG-3', { tipo: 'REVERSO', reversaDe: 'PAG-2' }),
      pago('PAG-4', { tipo: 'REVERSO', reversaDe: null }),
    ]
    const filas = filasDePagos(pagos)
    expect(filas.map((fila) => [fila.movimiento.id, fila.reverso?.id ?? null])).toEqual([
      ['PAG-1', null], ['PAG-2', 'PAG-3'], ['PAG-4', null],
    ])
    expect(tipoDelPago(pagos[2])).toBe('Reverso del abono')
    expect(tipoDelPago(pagos[3])).toBe('Devolución')
  })

  it('cambia el código del cargo corregido por su nombre, y muestra la cantidad (DEC-107)', () => {
    expect(conceptoVisible(CARGOS[4], CARGOS)).toBe('Corrección de Lavandería: se cargó a la habitación equivocada')
    expect(conceptoVisible(CARGOS[2], CARGOS)).toBe('Agua × 2')
    const raro = cargo('CAR-9', { tipo: 'AJUSTE', concepto: 'Otro texto', corrigeA: 'CAR-5', sentido: 'AUMENTA' })
    expect(conceptoVisible(raro, CARGOS)).toBe('Otro texto')
  })

  it('distingue una corrección de un ajuste del sistema', () => {
    expect(tipoDelCargo(CARGOS[4])).toBe('Corrección · disminuye')
    expect(tipoDelCargo(cargo('CAR-8', { tipo: 'AJUSTE', sentido: 'AUMENTA' }))).toBe('Ajuste · aumenta')
  })

  it('suma los cargos netos: la corrección que disminuye resta', () => {
    expect(cargosNetos(folio())).toBe(1350000 + 60000 + 10000 + 45000 - 45000 + 38000)
  })

  it('suma los consumos netos de sus correcciones y sin los extras', () => {
    expect(otrosCargos(folio())).toBe(10000 + 38000)
  })
})

describe('Qué se puede hacer con cada movimiento', () => {
  const filas = filasDeCargos(CARGOS)

  it('un consumo se corrige y se revierte con la reserva en curso', () => {
    expect(accionesDelCargo(filas[2], 'EN_CURSO', folio())).toEqual({ revertir: true, corregir: true })
  })

  it('hospedaje y extras solo se revierten: no se vuelven a cargar desde el folio', () => {
    expect(accionesDelCargo(filas[0], 'EN_CURSO', folio())).toEqual({ revertir: true, corregir: false })
    expect(accionesDelCargo(filas[1], 'EN_CURSO', folio())).toEqual({ revertir: true, corregir: false })
  })

  it('un cargo ya corregido, o con el folio cerrado, no admite nada', () => {
    expect(accionesDelCargo(filas[3], 'EN_CURSO', folio())).toEqual({ revertir: false, corregir: false })
    expect(accionesDelCargo(filas[2], 'EN_CURSO', folio({ cerrado: true }))).toEqual({ revertir: false, corregir: false })
  })

  it('fuera de la estancia un consumo ya no se corrige, pero se revierte', () => {
    expect(accionesDelCargo(filas[2], 'CONFIRMADA', folio())).toEqual({ revertir: true, corregir: false })
  })

  it('solo un abono sin revertir se revierte', () => {
    const [abono, revertido, devolucion] = filasDePagos([
      pago('PAG-1'), pago('PAG-2'), pago('PAG-3', { tipo: 'REVERSO', reversaDe: 'PAG-2' }), pago('PAG-4', { tipo: 'REVERSO' }),
    ])
    expect(puedeRevertirPago(abono, folio())).toBe(true)
    expect(puedeRevertirPago(revertido, folio())).toBe(false)
    expect(puedeRevertirPago(devolucion, folio())).toBe(false)
  })
})

describe('Acciones del folio según la reserva', () => {
  it('en curso con saldo pendiente: primero cobrar', () => {
    expect(accionesDelFolio('EN_CURSO', folio())).toEqual(['pago', 'consumo', 'salida'])
  })

  it('en curso al día: primero la salida', () => {
    expect(accionesDelFolio('EN_CURSO', folio({ situacionSaldo: 'AL_DIA', saldo: 0 }))).toEqual(['salida', 'pago', 'consumo'])
  })

  it('con saldo a favor: primero la devolución', () => {
    expect(accionesDelFolio('EN_CURSO', folio({ situacionSaldo: 'A_FAVOR' }))[0]).toBe('devolucion')
  })

  it('confirmada: sin consumos ni salida', () => {
    expect(accionesDelFolio('CONFIRMADA', folio())).toEqual(['pago'])
  })

  it('cancelada: cerrar primero, y cobrar solo si todavía debe', () => {
    expect(accionesDelFolio('CANCELADA', folio())).toEqual(['cerrar', 'pago'])
    expect(accionesDelFolio('NO_SHOW', folio({ situacionSaldo: 'A_FAVOR' }))).toEqual(['cerrar', 'devolucion'])
  })

  it('cerrado: ninguna', () => {
    expect(accionesDelFolio('FINALIZADA', folio({ cerrado: true }))).toEqual([])
  })
})
