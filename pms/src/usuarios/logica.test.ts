import { describe, expect, it } from 'vitest'
import {
  campoDelDetalle,
  codigoAlEscribir,
  errorDeCodigo,
  errorDeContrasena,
  errorDeCorreo,
  errorDeRepetida,
  estadoCuenta,
  nombreCorto,
  PERSONA_VACIA,
  validarPersona,
} from './logica'

describe('reglas de las cuentas', () => {
  it('la contraseña dice lo que le falta, y nada si cumple la regla del backend', () => {
    expect(errorDeContrasena('')).toBe('Escribe la contraseña nueva.')
    expect(errorDeContrasena('abc')).toBe('Le falta: al menos 8 caracteres, una mayúscula y un número.')
    expect(errorDeContrasena('abcdefgh1')).toBe('Le falta: una mayúscula.')
    expect(errorDeContrasena('Abcdefgh')).toBe('Le falta: un número.')
    expect(errorDeContrasena('Ñandú2026')).toBeUndefined()
    expect(errorDeContrasena('Salento2026')).toBeUndefined()
  })

  it('la repetida tiene que ser igual', () => {
    expect(errorDeRepetida('Salento2026', '')).toBe('Repite la contraseña nueva.')
    expect(errorDeRepetida('Salento2026', 'Salento2025')).toBe('Las dos contraseñas no son iguales.')
    expect(errorDeRepetida('Salento2026', 'Salento2026')).toBeUndefined()
  })

  it('el código son 6 números, y al pegarlo se quitan espacios y letras', () => {
    expect(codigoAlEscribir(' 730 214 ')).toBe('730214')
    expect(codigoAlEscribir('73021499')).toBe('730214')
    expect(errorDeCodigo('')).toBe('Escribe el código que te llegó al correo.')
    expect(errorDeCodigo('7302')).toBe('El código tiene 6 números.')
    expect(errorDeCodigo('730214')).toBeUndefined()
  })

  it('el correo necesita arroba y dominio', () => {
    expect(errorDeCorreo('')).toBe('Escribe el correo.')
    expect(errorDeCorreo('carlos.restrepo@ejemplo')).toBe('Escribe un correo válido, por ejemplo nombre@dominio.co.')
    expect(errorDeCorreo(' carlos.restrepo@ejemplo.co ')).toBeUndefined()
  })

  it('los datos personales son todos obligatorios y la fecha de nacimiento anterior a hoy', () => {
    expect(validarPersona(PERSONA_VACIA, '2026-11-17')).toEqual({
      nombre: 'Escribe el nombre.',
      telefono: 'Escribe el teléfono.',
      tipoDocumento: 'Elige el tipo de documento.',
      numeroDocumento: 'Escribe el número de documento.',
      fechaNacimiento: 'Elige la fecha de nacimiento.',
    })
    const completa = { nombre: 'Carlos Restrepo', telefono: '3001234567', tipoDocumento: 'CC' as const,
      numeroDocumento: '1094000000', fechaNacimiento: '1990-05-02' }
    expect(validarPersona(completa, '2026-11-17')).toEqual({})
    expect(validarPersona({ ...completa, fechaNacimiento: '2026-11-17' }, '2026-11-17').fechaNacimiento)
      .toBe('La fecha de nacimiento tiene que ser anterior a hoy.')
  })

  it('el estado de la cuenta sale de activado y eliminado', () => {
    expect(estadoCuenta({ activado: true, eliminado: false })).toBe('activa')
    expect(estadoCuenta({ activado: false, eliminado: false })).toBe('sin-activar')
    expect(estadoCuenta({ activado: true, eliminado: true })).toBe('eliminada')
  })

  it('ubica el campo de un detalle del 400 y acorta el nombre para los avisos', () => {
    expect(campoDelDetalle('telefono: es obligatorio', ['telefono', 'nombre'])).toBe('telefono')
    expect(campoDelDetalle('otro: no sé', ['telefono'])).toBeNull()
    expect(nombreCorto('María Fernanda Ospina')).toBe('María Fernanda')
    expect(nombreCorto('Carlos Restrepo')).toBe('Carlos')
  })
})
