import { describe, expect, it } from 'vitest'
import type { Titular } from '../api/titulares'
import { campoDelDetalle, cuerpoTitular, formDe, textoDocumento, textoValidacion, validarTitular } from './logica'

const ANA: Titular = {
  id: 'TIT-1', nombre: 'Ana María Ríos Pérez', nombres: 'Ana María', primerApellido: 'Ríos', segundoApellido: 'Pérez',
  tipoDocumento: 'CC', numeroDocumento: '1094000', correo: 'ana.rios@ejemplo.co', telefono: null,
  validado: true, validadoEn: '2026-11-14T15:24:00', validadoPor: 'USR-2', validadoPorNombre: 'María Fernanda Ospina',
}

describe('lógica de titulares', () => {
  it('muestra el documento con los miles separados, salvo un pasaporte', () => {
    expect(textoDocumento('CC', '1094000')).toBe('CC 1.094.000')
    expect(textoDocumento('PASAPORTE', 'AB123456')).toBe('Pasaporte AB123456')
    expect(textoDocumento('PASAPORTE', '123456789')).toBe('Pasaporte 123456789')
    expect(textoDocumento('CE', '4521889')).toBe('CE 4.521.889')
  })

  it('dice quién validó y cuándo, y sin nombre si esa cuenta ya no existe', () => {
    expect(textoValidacion(ANA)).toBe('Identidad validada · 14 nov 2026, 3:24 p. m., por María Fernanda Ospina')
    expect(textoValidacion({ ...ANA, validadoPorNombre: null })).toBe('Identidad validada · 14 nov 2026, 3:24 p. m.')
  })

  it('un titular de antes trae el nombre entero en Nombres para separar los apellidos', () => {
    expect(formDe(ANA)).toMatchObject({ nombres: 'Ana María', primerApellido: 'Ríos', segundoApellido: 'Pérez' })
    expect(formDe({ ...ANA, nombres: null, primerApellido: null, segundoApellido: null }))
      .toMatchObject({ nombres: 'Ana María Ríos Pérez', primerApellido: '', segundoApellido: '' })
  })

  it('pide los nombres y al menos un contacto, y revisa la forma del correo', () => {
    const vacio = { nombres: ' ', primerApellido: '', segundoApellido: '', correo: '', telefono: '' }
    expect(validarTitular(vacio)).toEqual({
      nombres: 'Escribe los nombres.',
      contacto: 'Deja al menos un correo o un teléfono.',
    })
    expect(validarTitular({ ...formDe(ANA), correo: 'ana@ejemplo' }).correo)
      .toBe('Escribe un correo válido, por ejemplo nombre@dominio.co.')
    expect(validarTitular({ ...formDe(ANA), correo: '', telefono: '300 123 4567' })).toEqual({})
  })

  it('manda lo opcional vacío como nulo y ubica el campo de un 400', () => {
    expect(cuerpoTitular({ ...formDe(ANA), segundoApellido: '  ', telefono: '' }))
      .toEqual({ nombres: 'Ana María', primerApellido: 'Ríos', segundoApellido: null, correo: 'ana.rios@ejemplo.co', telefono: null })
    expect(campoDelDetalle('correo: no tiene formato de correo')).toBe('correo')
    expect(campoDelDetalle('nombreRecibido: hace falta nombres o nombre')).toBeNull()
  })
})
