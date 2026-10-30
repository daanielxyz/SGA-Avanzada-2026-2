import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { ApartamentoPublico, Pagina } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'
import Inicio from './Inicio'

/** En el listado: un adulto, su próxima noche libre. */
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
    desglose: [{ noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 1, subtotal: 100000 }],
    total: 100000,
  },
  proximaNocheLibre: '2026-12-14',
}

/** En la búsqueda por fechas: dos noches, la segunda en temporada alta; pagan los 2 adultos. */
const SUITE_PARA_EL_GRUPO: ApartamentoPublico = {
  ...SUITE,
  precio: {
    desglose: [
      { noche: '2026-12-14', tarifa: 100000, ocupantesFacturables: 2, subtotal: 200000 },
      { noche: '2026-12-15', tarifa: 130000, ocupantesFacturables: 2, subtotal: 260000 },
    ],
    total: 460000,
  },
  proximaNocheLibre: null,
}

/** Una página del listado (`PaginaResponse`) con esos apartamentos. */
function pagina(...contenido: ApartamentoPublico[]): Pagina<ApartamentoPublico> {
  return { contenido, numero: 0, tamano: 10, totalElementos: contenido.length, totalPaginas: 1 }
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

function abrirInicio(umbral = 12) {
  render(
    <MemoryRouter>
      <Inicio umbral={umbral} />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Inicio', () => {
  it('al abrir muestra todos los apartamentos, con lo que paga un adulto su próxima noche libre', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)])
    abrirInicio()

    const tarjeta = await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(urlDeLaLlamada(fetch, 0)).toBe('/api/portal/apartamentos?pagina=0')
    expect(within(tarjeta).getByRole('img')).toHaveAttribute('src', 'https://cdn.sga.co/suite.jpg')
    expect(within(tarjeta).getByText('Hasta 4 personas · 2 dormitorios')).toBeInTheDocument()
    expect(within(tarjeta).getByRole('list', { name: 'Dotación' })).toHaveTextContent('BalcónCocinaWifiJacuzzi+1')
    expect(within(tarjeta).getByText(/\$\s*100\.000/)).toBeInTheDocument()
    expect(within(tarjeta).getByText('por adulto y noche')).toBeInTheDocument()
    expect(within(tarjeta).getByText(/noche del/)).toBeInTheDocument()
    expect(within(tarjeta).queryByText(/Total/)).not.toBeInTheDocument()
  })

  it('cada tarjeta lleva a la página del apartamento con la noche de su precio', async () => {
    laApiResponde([200, pagina(SUITE)])
    abrirInicio()

    expect(await screen.findByRole('link', { name: 'Suite Jardín' }))
      .toHaveAttribute('href', '/apartamentos/APT-101?entrada=2026-12-14')
  })

  it('sin foto, o si la foto no carga, queda un recuadro en su lugar y la fila no se desarma', async () => {
    laApiResponde([200, pagina({ ...SUITE, codigo: 'APT-7', nombre: 'A1', imagenes: [] }, SUITE)])
    abrirInicio()

    const [sinFoto, conFoto] = await screen.findAllByRole('article')
    expect(sinFoto.firstElementChild).toHaveClass('sin-foto')
    expect(sinFoto.children).toHaveLength(3)

    fireEvent.error(within(conFoto).getByRole('img'))
    expect(within(conFoto).queryByRole('img')).not.toBeInTheDocument()
    expect(conFoto.firstElementChild).toHaveClass('sin-foto')
  })

  it('el +N de la dotación dice cuáles faltan', async () => {
    laApiResponde([200, pagina(SUITE)])
    abrirInicio()

    expect(await screen.findByText('+1')).toHaveAttribute('title', 'Parqueadero')
  })

  it('un apartamento sin noches libres en el año se muestra sin precio', async () => {
    laApiResponde([200, pagina({ ...SUITE, precio: null, proximaNocheLibre: null })])
    abrirInicio()

    expect(await screen.findByText('Sin fechas libres por ahora')).toBeInTheDocument()
  })

  it('la edad desde la que se paga sale del alojamiento, no del código (DEC-83)', () => {
    laApiResponde([200, pagina()])
    abrirInicio(10)

    expect(screen.getByLabelText('Adultos')).toHaveAccessibleDescription('10 años o más')
    expect(screen.getByLabelText('Niños')).toHaveAccessibleDescription('Menores de 10, no pagan')
  })

  it('el filtro deja solo los libres, con el total del grupo, y se puede volver a ver todos', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)], [200, pagina(SUITE_PARA_EL_GRUPO)], [200, pagina(SUITE)])
    abrirInicio()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    fireEvent.change(screen.getByLabelText('Niños'), { target: { value: '1' } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    const tarjeta = await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(screen.getByRole('heading', { name: '1 apartamento libre' })).toBeInTheDocument()
    expect(urlDeLaLlamada(fetch, 1)).toBe(`/api/portal/apartamentos/disponibles?entrada=${fechaDesdeHoy(1)}`
      + `&salida=${fechaDesdeHoy(3)}&ocupantes=3&ninos=1&pagina=0`)
    expect(within(tarjeta).getByText(/\$\s*460\.000/)).toBeInTheDocument()
    expect(within(tarjeta).getByText('Total 2 noches, 2 adultos y 1 niño')).toBeInTheDocument()
    expect(within(tarjeta).getByText(/por adulto y noche/)).toHaveTextContent(/\$\s*100\.000 por adulto y noche/)
    expect(within(tarjeta).getByRole('link', { name: 'Suite Jardín' })).toHaveAttribute('href',
      `/apartamentos/APT-101?entrada=${fechaDesdeHoy(1)}&salida=${fechaDesdeHoy(3)}&adultos=2&ninos=1`)

    await userEvent.click(screen.getByRole('button', { name: 'Ver todos los apartamentos' }))
    expect(await screen.findByRole('heading', { name: 'Nuestros apartamentos' })).toBeInTheDocument()
  })

  it('con más de 10 apartamentos pasa de página en página (DEC-18)', async () => {
    const primera = { ...pagina(SUITE), totalElementos: 12, totalPaginas: 2 }
    const segunda = { ...pagina({ ...SUITE, codigo: 'APT-112', nombre: 'Loft Doce' }), numero: 1, totalElementos: 12,
      totalPaginas: 2 }
    const fetch = laApiResponde([200, primera], [200, segunda])
    abrirInicio()

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '← Anterior' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente →' }))

    expect(await screen.findByRole('article', { name: 'Loft Doce' })).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Siguiente →' })).toBeDisabled()
    expect(urlDeLaLlamada(fetch, 1)).toBe('/api/portal/apartamentos?pagina=1')
  })

  it('con una sola página no muestra la paginación', async () => {
    laApiResponde([200, pagina(SUITE)])
    abrirInicio()

    await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(screen.queryByRole('navigation', { name: 'Páginas' })).not.toBeInTheDocument()
  })

  it('si no hay libres lo dice y sugiere cambiar la búsqueda', async () => {
    laApiResponde([200, pagina(SUITE)], [200, pagina()])
    abrirInicio()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: 'No hay apartamentos libres' })).toBeInTheDocument()
    expect(screen.getByText(/Prueba con otras fechas/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando la búsqueda falla', async () => {
    laApiResponde([200, pagina(SUITE)],
      [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La estancia no es válida', detalles: [] }])
    abrirInicio()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La estancia no es válida')
  })

  it('no llama a la API si la salida no es después de la llegada', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)])
    abrirInicio()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    fireEvent.change(screen.getByLabelText('Salida'), { target: { value: fechaDesdeHoy(1) } })
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('La salida debe ser al menos un día después de la llegada.')
    expect(fetch).toHaveBeenCalledTimes(1)
  })
})
