import { describe, expect, it } from 'vitest'
import { cantidad, dinero, edadEn, fechaCorta, hora, horaEnPalabras, iniciales, noches, textoEdad } from './formato'

describe('formatos del PMS', () => {
  it('la fecha no se corre un día con la zona horaria', () => {
    expect(fechaCorta('2026-11-14')).toBe('14 nov 2026')
    expect(fechaCorta('2026-01-01T23:59:00')).toBe('1 ene 2026')
  })

  it('lo que no es una fecha se muestra tal cual', () => {
    expect(fechaCorta('xxx')).toBe('xxx')
  })

  it('las iniciales aguantan nombres poco comunes', () => {
    expect(iniciales('María Constanza Rodríguez')).toBe('MC')
    expect(iniciales('Jo')).toBe('J')
    expect(iniciales('  Sam   Lee ')).toBe('SL')
    expect(iniciales('Ólafur Ólafsson')).toBe('ÓÓ')
    expect(iniciales('🦊 Fox')).toBe('🦊F')
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

  it('antes del primer año la edad va en meses, como bebé', () => {
    expect(textoEdad('2026-03-20', '2026-11-14')).toBe('7 meses (bebé)')
    expect(textoEdad('2025-11-14', '2026-11-13')).toBe('11 meses (bebé)')
    expect(textoEdad('2026-10-14', '2026-11-14')).toBe('1 mes (bebé)')
    expect(textoEdad('2026-11-01', '2026-11-14')).toBe('Menos de un mes (bebé)')
    expect(textoEdad('2025-11-14', '2026-11-14')).toBe('1 año')
    expect(textoEdad('1988-03-14', '2026-11-14')).toBe('38 años')
  })

  it('la hora de un campo se dice en palabras, sin duda entre 24 horas y a. m./p. m.', () => {
    expect(horaEnPalabras('15:00')).toBe('Queda a las 3:00 p. m., en la tarde.')
    expect(horaEnPalabras('03:30')).toBe('Queda a las 3:30 a. m., en la madrugada.')
    expect(horaEnPalabras('21:15')).toBe('Queda a las 9:15 p. m., en la noche.')
    expect(horaEnPalabras('')).toContain('a. m. o p. m.')
  })

  it('el singular y el plural salen bien', () => {
    expect(cantidad(1, 'ocupante', 'ocupantes')).toBe('1 ocupante')
    expect(cantidad(2, 'ocupante', 'ocupantes')).toBe('2 ocupantes')
  })
})
