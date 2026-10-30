import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { ApartamentoPublico, EstanciaPortal } from '../api/apartamentos'
import Apartamento from './Apartamento'

const SUITE: ApartamentoPublico = {
  codigo: 'APT-101',
  nombre: 'Suite Jardín',
  descripcion: 'Vista a las montañas',
  capacidad: 4,
  dormitorios: 2,
  imagenes: [
    { url: 'https://cdn.sga.co/otra.jpg', principal: false },
    { url: 'https://cdn.sga.co/suite.jpg', principal: true },
  ],
  caracteristicas: ['Balcón', 'Cocina', 'Wifi', 'Jacuzzi'],
  precio: {
    desglose: [{ noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 }],
    total: 100000,
  },
  proximaNocheLibre: '2026-12-14',
}

const UNA_NOCHE_LIBRE: EstanciaPortal = {
  disponible: true,
  precio: {
    desglose: [{ noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 }],
    total: 100000,
  },
  proximaNocheLibre: '2026-12-14',
}

/** La API simulada responde, en orden, lo que se le pase: un cuerpo JSON y su estado HTTP. */
function laApiResponde(...respuestas: [number, unknown][]) {
  const fetch = vi.fn()
  for (const [estado, cuerpo] of respuestas) {
    fetch.mockResolvedValueOnce(new Response(JSON.stringify(cuerpo), { status: estado }))
  }
  vi.stubGlobal('fetch', fetch)
  return fetch
}

function urlDeLaLlamada(fetch: ReturnType<typeof vi.fn>, n: number) {
  return (fetch.mock.calls[n] as [string])[0]
}

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <Routes>
        <Route path="/apartamentos/:codigo" element={<Apartamento umbral={12} />} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Apartamento', () => {
  it('sin fechas abre con su próxima noche libre y un adulto, y muestra todas las fotos y la dotación', async () => {
    const fetch = laApiResponde([200, SUITE], [200, UNA_NOCHE_LIBRE])
    abrir('/apartamentos/APT-101')

    expect(await screen.findByRole('heading', { level: 1, name: 'Suite Jardín' })).toBeInTheDocument()
    expect(screen.getAllByRole('img').map((i) => i.getAttribute('src')))
      .toEqual(['https://cdn.sga.co/suite.jpg', 'https://cdn.sga.co/otra.jpg'])
    expect(screen.getByRole('list', { name: 'Dotación' })).toHaveTextContent('BalcónCocinaWifiJacuzzi')
    expect(screen.getByLabelText('Llegada')).toHaveValue('2026-12-14')
    expect(screen.getByLabelText('Salida')).toHaveValue('2026-12-15')
    expect(screen.getByLabelText('Adultos')).toHaveValue(1)
    expect(await screen.findByText(/por adulto, noche del/)).toHaveTextContent(/\$\s*100\.000 por adulto/)
    expect(urlDeLaLlamada(fetch, 1)).toBe(
      '/api/portal/apartamentos/APT-101/estancia?entrada=2026-12-14&salida=2026-12-15&ocupantes=1&ninos=0')
  })

  it('con la dirección de la búsqueda usa sus fechas y su grupo, y muestra el detalle noche por noche', async () => {
    const dosNoches: EstanciaPortal = {
      disponible: true,
      precio: {
        desglose: [
          { noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 2, subtotal: 200000 },
          { noche: '2026-12-15', tarifa: 130000, ocupantesFacturables: 2, subtotal: 260000 },
        ],
        total: 460000,
      },
      proximaNocheLibre: '2026-12-14',
    }
    const fetch = laApiResponde([200, SUITE], [200, dosNoches])
    abrir('/apartamentos/APT-101?entrada=2026-12-14&salida=2026-12-16&adultos=2&ninos=1')

    const detalle = await screen.findByRole('table', { name: 'Precio noche por noche' })
    expect(within(detalle).getAllByRole('row')[2]).toHaveTextContent(/\$\s*130\.000/)
    expect(screen.getByText(/Total 2 noches/)).toHaveTextContent(/\$\s*460\.000/)
    expect(urlDeLaLlamada(fetch, 1)).toBe(
      '/api/portal/apartamentos/APT-101/estancia?entrada=2026-12-14&salida=2026-12-16&ocupantes=3&ninos=1')
  })

  it('si no está libre lo dice y ofrece correr las fechas a la próxima noche libre', async () => {
    const ocupada: EstanciaPortal = { ...UNA_NOCHE_LIBRE, disponible: false, proximaNocheLibre: '2026-12-20' }
    const fetch = laApiResponde([200, SUITE], [200, ocupada], [200, UNA_NOCHE_LIBRE])
    abrir('/apartamentos/APT-101?entrada=2026-12-14&salida=2026-12-16')

    expect(await screen.findByText('No está libre en esas fechas.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Ver desde el/ }))

    expect(screen.getByLabelText('Llegada')).toHaveValue('2026-12-20')
    expect(screen.getByLabelText('Salida')).toHaveValue('2026-12-22')
    expect(await screen.findByText(/por adulto, noche del/)).toBeInTheDocument()
    expect(urlDeLaLlamada(fetch, 2)).toContain('entrada=2026-12-20&salida=2026-12-22')
  })

  it('sin noches libres en el año no culpa a las fechas elegidas', async () => {
    const sinNoches: EstanciaPortal = { ...UNA_NOCHE_LIBRE, disponible: false, proximaNocheLibre: null }
    laApiResponde([200, { ...SUITE, precio: null, proximaNocheLibre: null }], [200, sinNoches])
    abrir('/apartamentos/APT-101')

    expect(await screen.findByText('Este apartamento no tiene fechas libres en el próximo año.')).toBeInTheDocument()
    expect(screen.queryByText('No está libre en esas fechas.')).not.toBeInTheDocument()
  })

  it('los campos de personas llegan hasta la capacidad del apartamento', async () => {
    laApiResponde([200, SUITE], [200, UNA_NOCHE_LIBRE])
    abrir('/apartamentos/APT-101')

    expect(await screen.findByLabelText('Adultos')).toHaveAttribute('max', '4')
    expect(screen.getByLabelText('Niños')).toHaveAttribute('max', '4')
  })

  it('si el grupo no cabe lo dice sin llamar a la API', async () => {
    const fetch = laApiResponde([200, SUITE], [200, UNA_NOCHE_LIBRE])
    abrir('/apartamentos/APT-101')
    await screen.findByText(/por adulto, noche del/)

    fireEvent.change(screen.getByLabelText('Adultos'), { target: { value: '5' } })

    expect(screen.getByRole('alert')).toHaveTextContent('Este apartamento admite hasta 4 personas.')
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('un apartamento que no existe o ya no se vende dice que no lo encontramos', async () => {
    laApiResponde([404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe', detalles: [] }])
    abrir('/apartamentos/APT-999')

    expect(await screen.findByRole('heading', { name: 'No encontramos este apartamento' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver todos los apartamentos' })).toHaveAttribute('href', '/')
  })
})
