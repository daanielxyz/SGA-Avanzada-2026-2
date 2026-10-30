import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { ApartamentoPublico } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'
import Inicio from './Inicio'

/** Dos noches, la segunda en temporada alta; pagan los 2 adultos. */
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
  caracteristicas: ['Balcón', 'Cocina', 'Wifi', 'Jacuzzi', 'Parqueadero'],
  precio: {
    desglose: [
      { noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 2, subtotal: 200000 },
      { noche: '2026-12-15', tarifa: 130000, ocupantesFacturables: 2, subtotal: 260000 },
    ],
    total: 460000,
  },
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

afterEach(() => vi.unstubAllGlobals())

describe('Inicio', () => {
  it('la edad desde la que se paga sale del alojamiento, no del código (DEC-83)', () => {
    render(<Inicio umbral={10} />)

    expect(screen.getByLabelText('Adultos')).toHaveAccessibleDescription('10 años o más')
    expect(screen.getByLabelText('Niños')).toHaveAccessibleDescription('Menores de 10, no pagan')
  })

  it('busca con adultos y niños: todos cuentan para el cupo y la API sabe cuántos no pagan', async () => {
    const fetch = laApiResponde([200, [SUITE]])
    render(<Inicio umbral={12} />)

    fireEvent.change(screen.getByLabelText('Adultos'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Niños'), { target: { value: '1' } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: '1 apartamento libre' })).toBeInTheDocument()
    expect(urlDeLaLlamada(fetch, 0)).toBe(`/api/portal/apartamentos/disponibles?entrada=${fechaDesdeHoy(1)}`
      + `&salida=${fechaDesdeHoy(3)}&ocupantes=3&ninos=1`)
  })

  it('cada tarjeta muestra lo público, lo que paga un adulto la noche de llegada y el total del grupo', async () => {
    laApiResponde([200, [SUITE]])
    render(<Inicio umbral={12} />)
    fireEvent.change(screen.getByLabelText('Niños'), { target: { value: '1' } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    const tarjeta = await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(within(tarjeta).getByRole('img')).toHaveAttribute('src', 'https://cdn.sga.co/suite.jpg')
    expect(within(tarjeta).getByText('Hasta 4 personas · 2 dormitorios')).toBeInTheDocument()
    expect(within(tarjeta).getByRole('list', { name: 'Dotación' })).toHaveTextContent('BalcónCocinaWifi+2 más')
    expect(within(tarjeta).getByText(/por adulto, noche del/)).toHaveTextContent(/\$\s*100\.000 por adulto/)
    expect(within(tarjeta).getByText(/Total 2 noches/)).toHaveTextContent(
      /Total 2 noches, 2 adultos y 1 niño:\s*\$\s*460\.000/)
  })

  it('el detalle muestra el precio noche por noche, con la temporada de cada una', async () => {
    laApiResponde([200, [SUITE]])
    render(<Inicio umbral={12} />)
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await userEvent.click(await screen.findByRole('button', { name: 'Ver detalle por noche' }))

    const detalle = screen.getByRole('table', { name: 'Precio noche por noche' })
    expect(within(detalle).getAllByRole('row')).toHaveLength(4)
    expect(within(detalle).getAllByRole('row')[2]).toHaveTextContent(/\$\s*130\.000/)
    expect(within(detalle).getByRole('row', { name: /Total/ })).toHaveTextContent(/Total\s*\$\s*460\.000/)
  })

  it('un apartamento sin precio se muestra igual, con el precio por confirmar', async () => {
    laApiResponde([200, [{ ...SUITE, precio: null }]])
    render(<Inicio umbral={12} />)
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Precio por confirmar')).toBeInTheDocument()
  })

  it('si no hay libres lo dice y sugiere cambiar la búsqueda', async () => {
    laApiResponde([200, []])
    render(<Inicio umbral={12} />)

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: 'No hay apartamentos libres' })).toBeInTheDocument()
    expect(screen.getByText(/Prueba con otras fechas/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando la búsqueda falla', async () => {
    laApiResponde([409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La estancia no es válida', detalles: [] }])
    render(<Inicio umbral={12} />)

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La estancia no es válida')
  })

  it('no llama a la API si la salida no es después de la llegada', async () => {
    const fetch = laApiResponde()
    render(<Inicio umbral={12} />)

    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: fechaDesdeHoy(1) } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('La salida debe ser al menos un día después de la llegada.')
    expect(fetch).not.toHaveBeenCalled()
  })
})
