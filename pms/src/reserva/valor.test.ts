import { describe, expect, it } from 'vitest'
import type { Valor } from '../api/reservas'
import { lineaIva, totalConIva } from './valor'

const NOCHES: Valor['desglose'] = []

describe('valor de la estancia con IVA (DEC-134)', () => {
  it('con IVA: el total es el de la API y la línea dice el porcentaje', () => {
    const valor: Valor = { desglose: NOCHES, total: 10000, ivaPorcentaje: 19, ivaExento: false, iva: 1900, totalConIva: 11900 }
    expect(totalConIva(valor)).toBe(11900)
    expect(lineaIva(valor)).toEqual({ texto: 'IVA (19 %)', valor: 1900 })
  })

  it('exenta por pasaporte: la línea lo dice y vale cero', () => {
    const valor: Valor = { desglose: NOCHES, total: 10000, ivaPorcentaje: 19, ivaExento: true, iva: 0, totalConIva: 10000 }
    expect(lineaIva(valor)).toEqual({ texto: 'IVA (19 %) · exento por pasaporte', valor: 0 })
  })

  it('sin IVA o con una respuesta de antes del IVA: el total es la base y no hay línea', () => {
    expect(lineaIva({ desglose: NOCHES, total: 10000, ivaPorcentaje: 0, iva: 0, totalConIva: 10000 })).toBeNull()
    expect(totalConIva({ desglose: NOCHES, total: 10000 })).toBe(10000)
    expect(lineaIva({ desglose: NOCHES, total: 10000 })).toBeNull()
  })
})
