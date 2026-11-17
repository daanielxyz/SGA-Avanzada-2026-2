import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Alojamiento } from '../api/alojamiento'
import Politicas from './Politicas'

const HOTEL: Alojamiento = {
  nombre: 'Hotel de Prueba', direccion: 'Calle 5 # 3-10', ciudad: 'Salento', rnt: '12345',
  parametros: { umbralEdadFacturable: 12 },
}
const POLITICA = {
  tramos: [{ antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } }],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

function laApiResponde(politica: unknown, alojamiento: unknown = HOTEL) {
  vi.stubGlobal('fetch', vi.fn((ruta: string) => {
    const cuerpo = ruta === '/api/alojamiento' ? alojamiento : ruta === '/api/portal/politica' ? politica
      : { contenido: [], numero: 0, tamano: 10, totalElementos: 0, totalPaginas: 0 }
    return Promise.resolve(cuerpo === null ? new Response('{}', { status: 500 }) : new Response(JSON.stringify(cuerpo)))
  }))
}

function abrir(alojamiento: Alojamiento | null = HOTEL) {
  render(
    <MemoryRouter>
      <Politicas alojamiento={alojamiento} />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Políticas', () => {
  it('muestra la cancelación del hotel, que es la misma que se acepta al reservar (DEC-41)', async () => {
    laApiResponde(POLITICA)
    abrir()

    const cancelacion = screen.getByRole('region', { name: 'Cancelación' })
    expect(await within(cancelacion).findByText(/Si no llegas:/)).toBeInTheDocument()
    expect(cancelacion).toHaveTextContent('Con 48 horas o más de antelación: no se retiene nada.')
  })

  it('si la política no carga lo dice, y el resto de la página sigue', async () => {
    laApiResponde(null)
    abrir()

    expect(await screen.findByText(/No pudimos cargar la política/)).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Cookies' })).toHaveTextContent('no usa cookies')
  })

  it('el responsable de los datos y el registro de turismo son los del hotel (DEC-83 · DEC-130)', () => {
    laApiResponde(POLITICA)
    abrir()

    expect(screen.getByRole('region', { name: 'Tratamiento de datos personales' }))
      .toHaveTextContent('Hotel de Prueba, Calle 5 # 3-10, Salento, es responsable de los datos')
    expect(screen.getByRole('region', { name: 'Registro Nacional de Turismo' })).toHaveTextContent('con el número 12345')
  })

  it('sin número de registro, no inventa uno', () => {
    laApiResponde(POLITICA)
    abrir({ ...HOTEL, rnt: null })

    expect(screen.getByRole('region', { name: 'Registro Nacional de Turismo' }))
      .toHaveTextContent('todavía no ha publicado su número')
  })

  it('el pie de todas las páginas lleva a cada política y al cambiar de página se ve desde arriba', async () => {
    laApiResponde(POLITICA)
    const subir = vi.spyOn(window, 'scrollTo')
    render(
      <MemoryRouter initialEntries={['/apartamentos']}>
        <App />
      </MemoryRouter>,
    )

    const politicas = await screen.findByRole('navigation', { name: 'Políticas' })
    expect(within(politicas).getByRole('link', { name: 'Tratamiento de datos' })).toHaveAttribute('href', '/politicas#datos')
    expect(within(politicas).getByRole('link', { name: 'Cookies' })).toHaveAttribute('href', '/politicas#cookies')
    expect(await screen.findByText('Registro Nacional de Turismo 12345')).toBeInTheDocument()
    expect(subir).toHaveBeenCalledWith({ top: 0 })
  })
})
