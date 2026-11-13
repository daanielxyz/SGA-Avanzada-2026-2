import { describe, expect, it } from 'vitest'
import type { Politica } from '../api/politicas'
import { cuerpo, formDesde, hayErrores, validar, type FormPolitica } from './formulario'

const VIGENTE: Politica = {
  id: 'POL-3',
  version: 3,
  tramos: [
    { antelacionMinHoras: 0, penalizacion: { base: 'ANTICIPO_EXIGIDO', porcentaje: 100, montoFijo: null } },
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
    { antelacionMinHoras: 24, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'PAGADO', porcentaje: 100, montoFijo: null },
}

function conTramos(form: FormPolitica, cambios: Partial<FormPolitica['tramos'][number]>[]): FormPolitica {
  return { ...form, tramos: form.tramos.map((tramo, i) => ({ ...tramo, ...cambios[i] })) }
}

describe('publicar una versión nueva (POL-01 · POL-02 · POL-03)', () => {
  it('parte de la vigente, de la mayor antelación a la menor, y la envía igual', () => {
    const form = formDesde(VIGENTE)
    expect(form.tramos.map((t) => t.horas)).toEqual(['48', '24', '0'])
    expect(hayErrores(validar(form, 2))).toBe(false)
    expect(cuerpo(form)).toEqual({
      tramos: [
        { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0 } },
        { antelacionMinHoras: 24, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50 } },
        { antelacionMinHoras: 0, penalizacion: { base: 'ANTICIPO_EXIGIDO', porcentaje: 100 } },
      ],
      penalizacionNoShow: { base: 'PAGADO', porcentaje: 100 },
    })
  })

  it('sin vigente, parte de un tramo desde 0 horas', () => {
    expect(cuerpo(formDesde(null)).tramos).toEqual([{ antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 100 } }])
  })

  it('marca los dos tramos con las mismas horas y que falta el de 0 horas', () => {
    const form = conTramos(formDesde(VIGENTE), [{}, {}, { horas: '24' }])
    const errores = validar(form, 2)
    const [, segundo, tercero] = form.tramos
    expect(errores.tramos[segundo.clave].horas).toBe('Otro tramo también empieza en 24 horas. Cambia las horas de uno de los dos.')
    expect(errores.tramos[tercero.clave].horas).toBe(errores.tramos[segundo.clave].horas)
    expect(errores.lista).toEqual(['Falta el tramo desde 0 horas: pon 0 en las horas de uno de los tramos o agrega uno.'])
  })

  it('exige el mínimo de tramos del alojamiento', () => {
    const form = { ...formDesde(VIGENTE), tramos: formDesde(VIGENTE).tramos.slice(2) }
    expect(validar(form, 2).lista).toEqual(['Hacen falta al menos 2 tramos. Agrega otro tramo.'])
  })

  it('un porcentaje va de 0 a 100 y un monto fijo es mayor que 0, con puntos de mil', () => {
    const form = conTramos(formDesde(VIGENTE), [{ valor: '120' }, { valor: '0', unidad: 'monto' }, { valor: '50.000', unidad: 'monto' }])
    const errores = validar(form, 2)
    expect(errores.tramos[form.tramos[0].clave].valor).toBe('Escribe un porcentaje entero de 0 a 100.')
    expect(errores.tramos[form.tramos[1].clave].valor).toBe('Un monto fijo debe ser mayor que 0. Para no retener nada, usa 0 %.')
    expect(errores.tramos[form.tramos[2].clave]).toBeUndefined()
    expect(cuerpo(form).tramos[2].penalizacion).toEqual({ base: 'ANTICIPO_EXIGIDO', montoFijo: 50000 })
  })

  it('un porcentaje con punto no se lee como miles', () => {
    const form = conTramos(formDesde(VIGENTE), [{ valor: '1.5' }])
    expect(validar(form, 2).tramos[form.tramos[0].clave].valor).toBe('Escribe un porcentaje entero de 0 a 100.')
  })
})
