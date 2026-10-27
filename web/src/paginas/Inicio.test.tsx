import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { ApartamentoPublico, Cotizacion } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'
import Inicio from './Inicio'

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
  caracteristicas: ['Balcón', 'Cocina'],
}

const DOS_NOCHES: Cotizacion = {
  desglose: [
    { noche: '2026-12-20', temporada: 'TEM-1', tarifa: 100000, ocupantesFacturables: 2, subtotal: 200000 },
    { noche: '2026-12-21', temporada: 'TEM-1', tarifa: 100000, ocupantesFacturables: 2, subtotal: 200000 },
  ],
  total: 400000,
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

function llamada(fetch: ReturnType<typeof vi.fn>, n: number) {
  const [url, opciones] = fetch.mock.calls[n] as [string, RequestInit | undefined]
  return { url, cuerpo: opciones?.body ? JSON.parse(String(opciones.body)) : undefined }
}

afterEach(() => vi.unstubAllGlobals())

describe('Inicio', () => {
  it('busca con las fechas y personas y muestra solo los datos públicos de cada libre', async () => {
    const fetch = laApiResponde([200, [SUITE]])
    render(<Inicio />)

    fireEvent.change(screen.getByLabelText('Personas'), { target: { value: '3' } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: '1 apartamento libre' })).toBeInTheDocument()
    const tarjeta = screen.getByRole('article', { name: 'Suite Jardín' })
    expect(within(tarjeta).getByRole('img')).toHaveAttribute('src', 'https://cdn.sga.co/suite.jpg')
    expect(within(tarjeta).getByText('Hasta 4 personas · 2 dormitorios')).toBeInTheDocument()
    expect(within(tarjeta).getByRole('list', { name: 'Dotación' })).toHaveTextContent('BalcónCocina')
    expect(llamada(fetch, 0).url).toBe(
      `/api/portal/apartamentos/disponibles?entrada=${fechaDesdeHoy(1)}&salida=${fechaDesdeHoy(3)}&ocupantes=3`,
    )
  })

  it('si no hay libres lo dice y sugiere cambiar la búsqueda', async () => {
    laApiResponde([200, []])
    render(<Inicio />)

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: 'No hay apartamentos libres' })).toBeInTheDocument()
    expect(screen.getByText(/Prueba con otras fechas/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando la búsqueda falla', async () => {
    laApiResponde([409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La estancia no es válida', detalles: [] }])
    render(<Inicio />)

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La estancia no es válida')
  })

  it('no llama a la API si la salida no es después de la llegada', async () => {
    const fetch = laApiResponde()
    render(<Inicio />)

    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: fechaDesdeHoy(1) } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('La salida debe ser al menos un día después de la llegada.')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('pide la fecha de nacimiento de cada persona y muestra el precio noche por noche', async () => {
    const fetch = laApiResponde([200, [SUITE]], [200, DOS_NOCHES])
    render(<Inicio />)
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Ver precio' }))

    fireEvent.change(screen.getByLabelText('Fecha de nacimiento, persona 1'), { target: { value: '1990-05-01' } })
    fireEvent.change(screen.getByLabelText('Fecha de nacimiento, persona 2'), { target: { value: '1988-03-02' } })
    await userEvent.click(screen.getByRole('button', { name: 'Calcular precio' }))

    const precio = await screen.findByRole('table', { name: 'Precio de la estadía' })
    expect(within(precio).getAllByRole('row')).toHaveLength(4)
    expect(within(precio).getByRole('row', { name: /Total/ })).toHaveTextContent(/Total\s*\$\s*400\.000/)
    expect(llamada(fetch, 1)).toEqual({
      url: '/api/cotizaciones',
      cuerpo: {
        apartamento: 'APT-101',
        entrada: fechaDesdeHoy(1),
        salida: fechaDesdeHoy(3),
        fechasNacimiento: ['1990-05-01', '1988-03-02'],
      },
    })
  })

  it('sin todas las fechas de nacimiento no cotiza, y si el backend rechaza muestra por qué', async () => {
    const fetch = laApiResponde([200, [SUITE]], [
      409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Falta la tarifa de una noche', detalles: [] },
    ])
    render(<Inicio />)
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Ver precio' }))

    fireEvent.change(screen.getByLabelText('Fecha de nacimiento, persona 1'), { target: { value: '1990-05-01' } })
    await userEvent.click(screen.getByRole('button', { name: 'Calcular precio' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Escribe una fecha de nacimiento válida para cada persona.')
    expect(fetch).toHaveBeenCalledTimes(1)

    fireEvent.change(screen.getByLabelText('Fecha de nacimiento, persona 2'), { target: { value: '1988-03-02' } })
    await userEvent.click(screen.getByRole('button', { name: 'Calcular precio' }))
    expect(await screen.findByText('Falta la tarifa de una noche')).toBeInTheDocument()
  })
})
