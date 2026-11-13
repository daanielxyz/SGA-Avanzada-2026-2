import { describe, expect, it } from 'vitest'
import type { ReservaOcupada } from '../api/ocupacion'
import { cuentaDe, textoCortoDeCuenta, textoDeCuenta } from './cuenta'

function reserva(cambios: Partial<ReservaOcupada>): ReservaOcupada {
  return {
    codigo: 'RES-1', entrada: '2026-11-12', salida: '2026-11-15', estado: 'EN_CURSO', canalOrigen: 'DIRECTO',
    titularId: 'TIT-1', titularNombre: 'Ana Torres', saldo: 0, situacionSaldo: 'AL_DIA', totalPagado: 975000, ...cambios,
  }
}

describe('cuenta de la barra del tablero (BL-09 · DEC-101)', () => {
  it('el estado manda sobre la cuenta', () => {
    expect(cuentaDe(reserva({ estado: 'FINALIZADA', situacionSaldo: 'PENDIENTE', saldo: 50000 }))).toBe('salio')
    expect(cuentaDe(reserva({ estado: 'NO_SHOW', situacionSaldo: 'PENDIENTE' }))).toBe('no-show')
  })

  it('al día, a favor, con saldo y debe todo salen de la situación y de lo pagado', () => {
    expect(cuentaDe(reserva({}))).toBe('al-dia')
    expect(cuentaDe(reserva({ situacionSaldo: 'A_FAVOR', saldo: 40000 }))).toBe('al-dia')
    expect(cuentaDe(reserva({ situacionSaldo: 'PENDIENTE', saldo: 675000, totalPagado: 300000 }))).toBe('con-saldo')
    expect(cuentaDe(reserva({ situacionSaldo: 'PENDIENTE', saldo: 750000, totalPagado: 0 }))).toBe('debe-todo')
  })

  it('sin la cuenta en la respuesta no se adivina', () => {
    expect(cuentaDe(reserva({ situacionSaldo: undefined }))).toBeNull()
    expect(textoDeCuenta(reserva({ situacionSaldo: undefined }))).toBeNull()
  })

  it('dice la cuenta en palabras, con su valor en la descripción', () => {
    expect(textoCortoDeCuenta(reserva({ situacionSaldo: 'A_FAVOR', saldo: 40000 }))).toBe('A favor')
    expect(textoDeCuenta(reserva({ situacionSaldo: 'A_FAVOR', saldo: 40000 }))).toBe('A favor $ 40.000 · devolver')
    expect(textoDeCuenta(reserva({ situacionSaldo: 'PENDIENTE', saldo: 675000, totalPagado: 300000 }))).toBe('Con saldo: debe $ 675.000')
    expect(textoDeCuenta(reserva({ situacionSaldo: 'PENDIENTE', saldo: 750000, totalPagado: 0 }))).toBe('Debe todo: $ 750.000')
    expect(textoDeCuenta(reserva({ estado: 'FINALIZADA' }))).toBe('Ya salió')
  })
})
