import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <App />
    </MemoryRouter>,
  )
}

/** `GET /api/alojamiento` simulado: el nombre que la API trae, o que no responde. */
function laApiDiceQueElHotelEs(nombre: string | null) {
  vi.stubGlobal('fetch', vi.fn(() => nombre === null
    ? Promise.reject(new TypeError('sin red'))
    : Promise.resolve(new Response(JSON.stringify({ nombre, parametros: { umbralEdadFacturable: 12 } }),
      { status: 200 }))))
}

beforeEach(() => {
  document.title = 'Reservas'
  laApiDiceQueElHotelEs('Hotel de Prueba')
})

afterEach(() => vi.unstubAllGlobals())

describe('App', () => {
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
    expect(screen.queryByRole('banner')).toBeEmptyDOMElement()
    expect(document.title).toBe('Reservas')
  })

  it('una dirección que no existe muestra la página de no encontrada', () => {
    abrir('/no-existe')

    expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
