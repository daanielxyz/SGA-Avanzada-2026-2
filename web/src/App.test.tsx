import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { aplicarMarca } from './marca'

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <App />
    </MemoryRouter>,
  )
}

/** La API simulada: el alojamiento con ese nombre, ningún apartamento y una política sin tramos, o una API que no responde. */
const SIN_TRAMOS = { tramos: [], penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null } }
const SIN_APARTAMENTOS = { contenido: [], numero: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 }

function respuesta(ruta: string, alojamiento: unknown) {
  const cuerpo = ruta === '/api/alojamiento' ? alojamiento : ruta === '/api/portal/politica' ? SIN_TRAMOS : SIN_APARTAMENTOS
  return Promise.resolve(new Response(JSON.stringify(cuerpo), { status: 200 }))
}

function laApiDiceQueElHotelEs(nombre: string | null) {
  vi.stubGlobal('fetch', vi.fn((ruta: string) => nombre === null
    ? Promise.reject(new TypeError('sin red'))
    : respuesta(ruta, { nombre, parametros: { umbralEdadFacturable: 12 } })))
}

beforeEach(() => {
  document.title = 'Reservas'
  laApiDiceQueElHotelEs('Hotel de Prueba')
})

afterEach(() => vi.unstubAllGlobals())

describe('App', () => {
  it('aplica la marca que trae la API (DEC-83)', async () => {
    vi.stubGlobal('fetch', vi.fn((ruta: string) => respuesta(ruta, { nombre: 'Hotel de Prueba',
      parametros: { umbralEdadFacturable: 12 },
      marca: { colores: { acento: '#123456' }, logoUrl: 'https://cdn.sga.co/logo.svg' } })))
    abrir('/')

    const nombre = await screen.findByRole('link', { name: 'Hotel de Prueba' })
    expect(nombre.querySelector('img')).toHaveAttribute('src', 'https://cdn.sga.co/logo.svg')
    expect(document.documentElement.style.getPropertyValue('--acento')).toBe('#123456')
    aplicarMarca(null)
  })

  it('muestra la página de inicio', () => {
    abrir('/')

    expect(screen.getByRole('heading', { name: 'Reserva tu estadía' })).toBeInTheDocument()
  })

  it('el nombre del hotel sale de la API, en la cabecera y en la pestaña (DEC-83)', async () => {
    abrir('/')

    expect(await screen.findByRole('link', { name: 'Hotel de Prueba' })).toHaveAttribute('href', '/')
    expect(document.title).toBe('Hotel de Prueba · Reservas')
  })

  it('si la API no responde, la web sigue y la pestaña dice Reservas', async () => {
    laApiDiceQueElHotelEs(null)
    abrir('/')

    expect(await screen.findByRole('heading', { name: 'Reserva tu estadía' })).toBeInTheDocument()
    expect(screen.getByRole('banner').querySelector('.marca')).toBeNull()
    expect(screen.getByRole('link', { name: 'Entrar' })).toBeInTheDocument()
    expect(document.title).toBe('Reservas')
  })

  it('la cabecera lleva a reservar desde cualquier página, salvo donde ya se está reservando', async () => {
    abrir('/')
    expect(await screen.findByRole('link', { name: 'Reservar' })).toHaveAttribute('href', '/apartamentos')
    cleanup()

    abrir('/apartamentos')
    expect(await screen.findByRole('heading', { level: 1, name: 'Apartamentos' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Reservar' })).not.toBeInTheDocument()
  })

  it('una dirección que no existe muestra la página de no encontrada', () => {
    abrir('/no-existe')

    expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
