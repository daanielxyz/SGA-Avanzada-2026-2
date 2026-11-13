import { describe, expect, it } from 'vitest'
import { ErrorApi } from './cliente'
import { detalleClaro, explicar, explicarConDetalles } from './mensajes'

type Extra = { motivo?: string; detalles?: string[]; estado?: number }

const error = (codigo: string, mensaje: string, extra: Extra = {}) =>
  new ErrorApi({ estado: extra.estado ?? 409, codigo, mensaje, detalles: extra.detalles ?? [], motivo: extra.motivo })

describe('Mensajes de error para el huésped', () => {
  it('traduce las reglas de negocio por su motivo, no por el texto técnico del backend', () => {
    expect(explicar(error('REGLA_NEGOCIO', 'Transición de reserva no permitida: CONFIRMADA → CANCELADA',
      { motivo: 'ESTADO_NO_PERMITIDO' }), '')).toMatch(/^Tu reserva cambió hace un momento/)
    expect(explicar(error('REGLA_NEGOCIO', 'El APT-101 no está disponible', { motivo: 'NO_DISPONIBLE' }), ''))
      .toMatch(/ya no están libres.*Elige otras fechas/)
    expect(explicar(error('REGLA_NEGOCIO', 'El grupo debe traer un mayor de edad que sea el huésped principal',
      { motivo: 'HUESPED_PRINCIPAL_INVALIDO' }), '')).toMatch(/al menos una persona mayor de edad/)
  })

  it('conserva el dato útil del backend y agrega qué hacer', () => {
    expect(explicar(error('REGLA_NEGOCIO', 'La estancia debe ser de al menos 3 noches en esta temporada',
      { motivo: 'ESTANCIA_MINIMA' }), ''))
      .toBe('La estancia debe ser de al menos 3 noches en esta temporada. Agrega noches o elige otras fechas.')
  })

  it('muestra tal cual lo que el backend ya escribe para el huésped', () => {
    expect(explicar(error('CREDENCIALES_INVALIDAS', 'Los datos o el PIN no son válidos, o el PIN ya venció',
      { estado: 401 }), '')).toBe('Los datos o el PIN no son válidos, o el PIN ya venció')
    expect(explicar(error('REGLA_NEGOCIO', 'El ocupante Ana Ríos está repetido'), '')).toBe(
      'El ocupante Ana Ríos está repetido')
  })

  it('las fallas del servidor y los cambios simultáneos se explican sin términos técnicos', () => {
    expect(explicar(error('ERROR_INTERNO', 'Error interno del servidor', { estado: 500 }), ''))
      .toMatch(/^Algo falló de nuestro lado/)
    expect(explicar(error('CONCURRENCIA', 'La reserva fue modificada por otra transacción'), ''))
      .toMatch(/Recarga la página/)
  })

  it('sin respuesta del backend usa lo que se intentaba hacer', () => {
    expect(explicar(new TypeError('Failed to fetch'), 'No pudimos crear tu reserva.'))
      .toBe('No pudimos crear tu reserva.')
  })

  it('los datos inválidos dicen qué revisar, con el nombre de cada campo en pantalla', () => {
    expect(explicarConDetalles(error('VALIDACION', 'La solicitud tiene datos inválidos', {
      estado: 400,
      detalles: ['titular.correo: no tiene formato de correo', 'ocupantes[1].fechaNacimiento: es obligatoria'],
    }), '')).toEqual({
      mensaje: 'Revisa estos datos:',
      detalles: ['Correo: no tiene formato de correo', 'Persona 2 · Fecha de nacimiento: es obligatoria'],
    })
  })

  it('un campo que no reconoce queda como llegó', () => {
    expect(detalleClaro('titular.nombreRecibido: hace falta nombres o nombre')).toBe('Nombres: es obligatorio')
    expect(detalleClaro('algoNuevo: no sirve')).toBe('algoNuevo: no sirve')
    expect(detalleClaro('sin separador')).toBe('sin separador')
  })
})
