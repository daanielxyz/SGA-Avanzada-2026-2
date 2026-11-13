import { describe, expect, it } from 'vitest'
import { PARAMETROS } from '../pruebas/alojamientoDePrueba'
import { contraste, textoContraste } from './contraste'
import {
  cuerpoMarca, cuerpoReglas, formMarca, formReglas, nombreMedio, repartirPegado, validarDatos, validarMarca,
  validarReglas, validarUbicacion,
} from './formularios'

describe('contraste de la marca (WCAG)', () => {
  it('negro sobre blanco es 21:1 y el mismo color es 1:1', () => {
    expect(contraste('#000000', '#FFFFFF')).toBeCloseTo(21, 5)
    expect(contraste('#C9762B', '#C9762B')).toBeCloseTo(1, 5)
  })

  it('no mide si falta un color válido', () => {
    expect(contraste('#FFF', '#000000')).toBeNull()
  })

  it('corta en vez de redondear, para no decir «4,5» de un 4,49', () => {
    expect(textoContraste(4.49)).toBe('4,4:1')
    expect(textoContraste(9.27)).toBe('9,2:1')
  })
})

describe('datos y ubicación (ALO-01 · ALO-04)', () => {
  it('pide los obligatorios y respeta los largos del contrato', () => {
    const errores = validarDatos({ nombre: '', descripcion: ' ', ciudad: 'Salento', direccion: 'x'.repeat(201), normas: '' })
    expect(errores).toEqual({
      nombre: 'Escribe el nombre del alojamiento.',
      descripcion: 'Escribe la descripción.',
      direccion: 'La dirección admite hasta 200 caracteres.',
    })
  })

  it('acepta coma decimal y rechaza coordenadas fuera de rango', () => {
    expect(validarUbicacion({ latitud: '4,6372', longitud: '-75.5701' })).toEqual({})
    expect(validarUbicacion({ latitud: '91', longitud: 'abc' })).toEqual({
      latitud: 'La latitud va de -90 a 90.',
      longitud: 'Escribe la longitud con números, por ejemplo -75.5701.',
    })
  })

  it('reparte lo que copia Google Maps entre latitud y longitud', () => {
    expect(repartirPegado('4.6372, -75.5701')).toEqual({ latitud: '4.6372', longitud: '-75.5701' })
    expect(repartirPegado('Salento')).toBeNull()
  })
})

describe('reglas de la estancia (CU-51)', () => {
  it('ida y vuelta: envía los 21 parámetros tal como llegaron', () => {
    const cuerpo = cuerpoReglas(formReglas(PARAMETROS))
    expect(Object.keys(cuerpo)).toHaveLength(21)
    expect(cuerpo).toEqual(PARAMETROS)
  })

  it('muestra los pesos con puntos de mil y los lee de vuelta', () => {
    const form = formReglas(PARAMETROS)
    expect(form.cargoCarro).toBe('15.000')
    expect(form.horaEntrada).toBe('15:00')
    expect(cuerpoReglas({ ...form, cargoCarro: '$ 18.500' }).cargoCarro).toBe(18500)
  })

  it('rechaza anticipo de más de 100, plazos en 0 y pesos con decimales', () => {
    const errores = validarReglas({
      ...formReglas(PARAMETROS), anticipoPct: '120', plazoConfirmacionHoras: '0', cargoMoto: '8000,5', minimoTramosCancelacion: '0',
    })
    expect(errores).toEqual({
      anticipoPct: 'El anticipo va de 0 a 100 %.',
      plazoConfirmacionHoras: 'Debe ser 1 o más.',
      minimoTramosCancelacion: 'Debe ser 1 o más.',
      cargoMoto: 'Escribe el valor en pesos, sin decimales.',
    })
  })

  it('los campos de mascotas ocultos no frenan el guardado', () => {
    const form = { ...formReglas(PARAMETROS), aceptaMascotas: false, limiteMascotas: 'dos' }
    expect(validarReglas(form)).toEqual({})
    expect(cuerpoReglas(form)).toMatchObject({ aceptaMascotas: false, limiteMascotas: 0, cargoMascota: 30000 })
  })
})

describe('marca (BL-05)', () => {
  const MARCA = {
    colores: {
      fondo: '#fbf7f0', superficie: '#FFFFFF', texto: '#2B2118', textoSuave: '#6E6257', borde: '#E6DDD0',
      bordeCampo: '#9C8E7E', acento: '#C9762B', textoSobreAcento: '#FFFFFF', peligro: '#B3261E', exito: '#2E7D32',
    },
    fuentes: { titulos: 'Lora', texto: 'Inter' },
    radio: { pequeno: '4px', mediano: '8px', grande: '16px' },
    logoUrl: null,
    portadaUrl: 'https://ejemplo.com/portada.jpg',
  }

  it('ida y vuelta, con los colores en mayúsculas y los radios en px', () => {
    expect(cuerpoMarca(formMarca(MARCA))).toEqual({ ...MARCA, colores: { ...MARCA.colores, fondo: '#FBF7F0' } })
  })

  it('valida colores, fuentes, radios y direcciones con los patrones del contrato', () => {
    const errores = validarMarca({
      ...formMarca(MARCA), acento: '#C976', fuenteTitulos: 'Lora; serif', radioGrande: '120', logoUrl: 'ftp://logo.png',
    })
    expect(Object.keys(errores).sort()).toEqual(['acento', 'fuenteTitulos', 'logoUrl', 'radioGrande'])
  })
})

it('dice los medios de pago en palabras', () => {
  expect(nombreMedio('PAGO_EN_LINEA')).toBe('Pago en línea')
  expect(nombreMedio('EFECTIVO')).toBe('Efectivo')
})
