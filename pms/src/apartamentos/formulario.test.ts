import { describe, expect, it } from 'vitest'
import type { Bloqueo, FichaApartamento } from '../api/apartamentos'
import {
  agregarCaracteristica,
  agregarFoto,
  campoDelDetalle,
  cuerpoNuevo,
  FORM_VACIO,
  quitarFoto,
  separarBloqueos,
  textoMascotas,
  validarApartamento,
  validarBloqueo,
} from './formulario'

const LLENO = {
  ...FORM_VACIO,
  numero: '111',
  nombre: '  Loft Central ',
  capacidad: '4',
  dormitorios: '2',
  caracteristicas: ['Wifi'],
  imagenes: [{ url: 'https://fotos.ejemplo.com/a.jpg', principal: true }],
}

describe('formulario de apartamento', () => {
  it('un apartamento completo pasa y se envía con «APT-» y los números como números', () => {
    expect(validarApartamento(LLENO, true)).toEqual({})
    expect(cuerpoNuevo(LLENO)).toEqual({
      codigo: 'APT-111', nombre: 'Loft Central', descripcion: '', capacidad: 4, dormitorios: 2,
      caracteristicas: ['Wifi'], imagenes: [{ url: 'https://fotos.ejemplo.com/a.jpg', principal: true }],
    })
  })

  it('al crear exige al menos una foto, una característica y números desde 1 (acordado el 13 nov)', () => {
    const errores = validarApartamento({ ...LLENO, numero: 'A1', capacidad: '0', caracteristicas: [], imagenes: [] }, true)
    expect(Object.keys(errores).sort()).toEqual(['capacidad', 'caracteristicas', 'imagenes', 'numero'])
    expect(errores.imagenes).toBe('Agrega al menos una foto.')
  })

  it('al editar no revisa el código, la capacidad ni las fotos, que ahí no se cambian', () => {
    expect(validarApartamento({ ...LLENO, numero: '', capacidad: '', imagenes: [] }, false)).toEqual({})
  })

  it('las características no se repiten, sin importar mayúsculas', () => {
    expect(agregarCaracteristica(['Wifi'], '  balcón  ')).toEqual(['Wifi', 'balcón'])
    expect(agregarCaracteristica(['Wifi'], 'WIFI')).toBe('Esa característica ya está.')
    expect(agregarCaracteristica([], '   ')).toMatch(/Escribe/)
  })

  it('el primer renglón de foto queda principal y, al quitar la principal, la toma la siguiente (IMG-02)', () => {
    const dos = agregarFoto(agregarFoto([]))
    expect(dos.map((i) => i.principal)).toEqual([true, false])
    const llenas = [{ url: 'https://f.co/1.jpg', principal: true }, { url: 'https://f.co/2.jpg', principal: false }]
    expect(quitarFoto(llenas, 0)).toEqual([{ url: 'https://f.co/2.jpg', principal: true }])
    expect(agregarFoto(Array.from({ length: 10 }, () => llenas[1]))).toHaveLength(10)
  })

  it('las direcciones deben ser enlaces y no repetirse', () => {
    const con = (urls: string[]) => validarApartamento({ ...LLENO,
      imagenes: urls.map((url, i) => ({ url, principal: i === 0 })) }, true).imagenes
    expect(con([''])).toBe('Agrega al menos una foto.')
    expect(con(['fotos/1.jpg'])).toMatch(/https:\/\//)
    expect(con(['https://f.co/1.jpg', 'https://f.co/1.jpg'])).toBe('Hay una foto repetida.')
  })

  it('cada detalle de un 400 va a su campo', () => {
    expect(campoDelDetalle('codigo: debe coincidir con "APT-\\d+"')).toBe('numero')
    expect(campoDelDetalle('imagenes[0].url: debe coincidir')).toBe('imagenes')
    expect(campoDelDetalle('caracteristicas[1]: el tamaño debe estar entre 0 y 100')).toBe('caracteristicas')
    expect(campoDelDetalle('alojamientoId: no puede estar vacío')).toBeNull()
  })
})

describe('bloqueos', () => {
  const bloqueo = (id: string, fechaInicio: string, fechaFin: string, vigente = true): Bloqueo =>
    ({ id, fechaInicio, fechaFin, motivo: 'Mantenimiento', vigente })

  it('arriba los que aún cuentan; abajo los levantados y los terminados (el fin es exclusivo)', () => {
    const { vigentes, anteriores } = separarBloqueos([
      bloqueo('B1', '2026-11-24', '2026-11-27'),
      bloqueo('B2', '2026-10-01', '2026-10-05'),
      bloqueo('B3', '2026-12-01', '2026-12-03', false),
      bloqueo('B4', '2026-11-10', '2026-11-17'),
      bloqueo('B5', '2026-11-20', '2026-11-22'),
    ], '2026-11-17')
    expect(vigentes.map((b) => b.id)).toEqual(['B5', 'B1'])
    expect(anteriores.map((b) => b.id)).toEqual(['B3', 'B4', 'B2'])
  })

  it('pide las dos fechas en orden y el motivo', () => {
    expect(validarBloqueo({ inicio: '2026-11-24', fin: '2026-11-24', motivo: ' ' })).toEqual({
      fin: '«Hasta» debe ser después de «Desde».',
      motivo: 'Escribe el motivo del bloqueo.',
    })
    expect(validarBloqueo({ inicio: '2026-11-24', fin: '2026-11-27', motivo: 'Pintura' })).toEqual({})
  })
})

describe('mascotas', () => {
  const con = (limiteMascotas: number | null) => ({ limiteMascotas }) as FichaApartamento

  it('el límite propio manda; si no tiene, dice que usa el del hotel', () => {
    expect(textoMascotas(con(2), 3)).toBe('Hasta 2 mascotas')
    expect(textoMascotas(con(1), 3)).toBe('Hasta 1 mascota')
    expect(textoMascotas(con(0), 3)).toBe('No admite mascotas')
    expect(textoMascotas(con(null), 3)).toBe('Hasta 3 mascotas (el límite del hotel)')
  })
})
