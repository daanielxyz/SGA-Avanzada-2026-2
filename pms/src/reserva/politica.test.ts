import { describe, expect, it } from 'vitest'
import type { Politica } from '../api/politicas'
import { resumenPolitica, textoRetencion, textoTramo, tramosOrdenados } from './politica'

const POLITICA: Politica = {
  id: 'POL-1',
  version: 1,
  tramos: [
    { antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

describe('política de cancelación en palabras (RN-13 · DEC-104)', () => {
  it('lee los tramos de mayor a menor antelación', () => {
    const tramos = tramosOrdenados(POLITICA)
    expect(textoTramo(tramos, 0)).toBe('48 horas o más antes de la entrada')
    expect(textoTramo(tramos, 1)).toBe('Menos de 48 horas antes de la entrada')
    expect(textoRetencion(tramos[0].penalizacion)).toBe('No se retiene nada')
    expect(textoRetencion(tramos[1].penalizacion)).toBe('Se retiene el 50 % del valor total')
    expect(textoRetencion(POLITICA.penalizacionNoShow)).toBe('Se retiene el 100 % del valor total')
  })

  it('un tramo intermedio dice entre cuántas horas rige', () => {
    const tramos = tramosOrdenados({
      ...POLITICA,
      tramos: [...POLITICA.tramos, { antelacionMinHoras: 24, penalizacion: { base: 'PAGADO', porcentaje: 30, montoFijo: null } }],
    })
    expect(textoTramo(tramos, 1)).toBe('Entre 24 y 48 horas antes de la entrada')
    expect(textoRetencion(tramos[1].penalizacion)).toBe('Se retiene el 30 % de lo pagado')
  })

  it('resume la política con el primer tramo que retiene', () => {
    expect(resumenPolitica(POLITICA)).toBe('Retiene el 50 % del valor total si se cancela con menos de 48 horas')
    expect(resumenPolitica({ ...POLITICA, tramos: [POLITICA.tramos[1]] })).toBe('No retiene nada al cancelar')
    expect(resumenPolitica({
      ...POLITICA,
      tramos: [{ antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: null, montoFijo: 100000 } }],
    })).toBe('Retiene $ 100.000 si se cancela')
  })
})
