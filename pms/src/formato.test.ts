import { describe, expect, it } from 'vitest'
import { cantidad, dinero, edadEn, fechaCorta, hora, noches } from './formato'

describe('formatos del PMS', () => {
  it('la fecha no se corre un día con la zona horaria', () => {
    expect(fechaCorta('2026-11-14')).toBe('14 nov 2026')
    expect(fechaCorta('2026-01-01T23:59:00')).toBe('1 ene 2026')
  })

  it('el dinero va en pesos, sin decimales y con punto de miles', () => {
    expect(dinero(1250000)).toBe('$ 1.250.000')
    expect(dinero(128640000)).toBe('$ 128.640.000')
    expect(dinero(0)).toBe('$ 0')
  })

  it('la hora va en formato de 12 horas', () => {
    expect(hora('16:30:00')).toBe('4:30 p. m.')
    expect(hora('00:05')).toBe('12:05 a. m.')
    expect(hora('12:00:00')).toBe('12:00 p. m.')
  })

  it('la noche de salida no cuenta (EST-01)', () => {
    expect(noches('2026-11-14', '2026-11-17')).toBe(3)
    expect(noches('2026-12-31', '2027-01-01')).toBe(1)
  })

  it('la edad cambia el día del cumpleaños, no antes', () => {
    expect(edadEn('2000-11-14', '2026-11-13')).toBe(25)
    expect(edadEn('2000-11-14', '2026-11-14')).toBe(26)
  })

  it('el singular y el plural salen bien', () => {
    expect(cantidad(1, 'ocupante', 'ocupantes')).toBe('1 ocupante')
    expect(cantidad(2, 'ocupante', 'ocupantes')).toBe('2 ocupantes')
  })
})
