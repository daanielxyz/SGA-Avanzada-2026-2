import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Alojamiento } from '../api/alojamiento'
import type { ApartamentoPublico, Pagina } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'
import Apartamentos from './Apartamentos'

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

function alojamiento(umbral: number, datos: Partial<Alojamiento> = {}): Alojamiento {
  return { nombre: 'Hotel de Prueba', parametros: { umbralEdadFacturable: umbral }, ...datos }
}

function abrirApartamentos(umbral = 12) {
  render(
    <MemoryRouter>
      <Apartamentos alojamiento={alojamiento(umbral)} />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Apartamentos', () => {
  it('al abrir muestra todos los apartamentos, con lo que paga un adulto su próxima noche libre', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)])
    abrirApartamentos()

    const tarjeta = await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(urlDeLaLlamada(fetch, 0)).toBe('/api/portal/apartamentos?pagina=0')
    expect(within(tarjeta).getByRole('img')).toHaveAttribute('src', 'https://cdn.sga.co/suite.jpg')
    expect(within(tarjeta).getByText('Hasta 4 personas · 2 dormitorios')).toBeInTheDocument()
    expect(within(tarjeta).getByRole('list', { name: 'Qué incluye' })).toHaveTextContent('BalcónCocinaWifiJacuzzi+1')
    expect(within(tarjeta).getByText(/\$\s*100\.000/)).toBeInTheDocument()
    expect(within(tarjeta).getByText('por adulto y noche')).toBeInTheDocument()
    expect(within(tarjeta).getByText(/libre desde el/)).toBeInTheDocument()
    expect(within(tarjeta).queryByText(/Total/)).not.toBeInTheDocument()
  })

  it('el resumen de la búsqueda se despliega y, al buscar, muestra las fechas', async () => {
    laApiResponde([200, pagina(SUITE)], [200, pagina(SUITE)])
    abrirApartamentos()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    const resumen = screen.getByRole('button', { name: /¿Cuándo vienes\?/ })
    expect(resumen).toHaveAttribute('aria-expanded', 'false')
    await userEvent.click(resumen)
    expect(resumen).toHaveAttribute('aria-expanded', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    await screen.findByRole('heading', { name: '1 apartamento libre' })
    expect(resumen).toHaveAttribute('aria-expanded', 'false')
    expect(resumen).not.toHaveTextContent('¿Cuándo vienes?')
  })

  it('el botón dice Buscando solo cuando alguien busca, no mientras carga la lista al abrir', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    abrirApartamentos()

    expect(screen.getByRole('button', { name: 'Buscar' })).toBeEnabled()
    expect(screen.getByText('Cargando apartamentos…')).toBeInTheDocument()
  })

  it('las fechas se eligen en el mismo calendario desplegable de la página del apartamento', async () => {
    laApiResponde([200, pagina()])
    abrirApartamentos()

    await userEvent.click(screen.getByRole('button', { name: /^Salida/ }))

    expect(screen.getByRole('dialog', { name: 'Elegir fechas' })).toBeInTheDocument()
  })

  it('sin la edad todavía, los campos de personas no muestran ayuda', () => {
    laApiResponde([200, pagina()])
    render(
      <MemoryRouter>
        <Apartamentos alojamiento={null} />
      </MemoryRouter>,
    )

    expect(screen.getByLabelText('Niños')).toHaveAccessibleDescription('')
  })

  it('cada tarjeta lleva a la página del apartamento con la noche de su precio', async () => {
    laApiResponde([200, pagina(SUITE)])
    abrirApartamentos()

    expect(await screen.findByRole('link', { name: 'Suite Jardín' }))
      .toHaveAttribute('href', '/apartamentos/APT-101?entrada=2026-12-14&salida=2026-12-15')
  })

  it('sin foto, o si la foto no carga, queda un recuadro en su lugar y la fila no se desarma', async () => {
    laApiResponde([200, pagina({ ...SUITE, codigo: 'APT-7', nombre: 'A1', imagenes: [] }, SUITE)])
    abrirApartamentos()

    const [sinFoto, conFoto] = await screen.findAllByRole('article')
    expect(sinFoto.firstElementChild).toHaveClass('sin-foto')
    expect(sinFoto.children).toHaveLength(3)

    fireEvent.error(within(conFoto).getByRole('img'))
    expect(within(conFoto).queryByRole('img')).not.toBeInTheDocument()
    expect(conFoto.firstElementChild).toHaveClass('sin-foto')
  })

  it('el +N de la dotación dice cuáles faltan', async () => {
    laApiResponde([200, pagina(SUITE)])
    abrirApartamentos()

    expect(await screen.findByText('+1')).toHaveAttribute('title', 'Parqueadero')
  })

  it('un apartamento sin noches libres en el año se muestra sin precio', async () => {
    laApiResponde([200, pagina({ ...SUITE, precio: null, proximaNocheLibre: null })])
    abrirApartamentos()

    expect(await screen.findByText('Sin fechas libres por ahora')).toBeInTheDocument()
  })

  it('la edad desde la que se paga sale del alojamiento, no del código (DEC-83)', () => {
    laApiResponde([200, pagina()])
    abrirApartamentos(10)

    expect(screen.getByLabelText('Adultos')).toHaveAccessibleDescription('10 años o más')
    expect(screen.getByLabelText('Niños')).toHaveAccessibleDescription('Menores de 10')
  })

  it('el filtro deja solo los libres, con el total del grupo, y se puede volver a ver todos', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)], [200, pagina(SUITE_PARA_EL_GRUPO)], [200, pagina(SUITE)])
    abrirApartamentos()
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
    abrirApartamentos()

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
    abrirApartamentos()

    await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(screen.queryByRole('navigation', { name: 'Páginas' })).not.toBeInTheDocument()
  })

  it('si no hay libres lo dice y sugiere cambiar la búsqueda', async () => {
    laApiResponde([200, pagina(SUITE)], [200, pagina()])
    abrirApartamentos()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: 'No hay apartamentos libres' })).toBeInTheDocument()
    expect(screen.getByText(/Prueba con otras fechas/)).toBeInTheDocument()
  })

  it('muestra el mensaje del backend cuando la búsqueda falla', async () => {
    laApiResponde([200, pagina(SUITE)],
      [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La estancia no es válida', detalles: [] }])
    abrirApartamentos()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La estancia no es válida')
  })

  it('no busca sin las dos fechas', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)])
    abrirApartamentos()
    await screen.findByRole('heading', { name: 'Nuestros apartamentos' })

    await userEvent.click(screen.getByRole('button', { name: /^Llegada/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar fechas' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Elige la llegada y la salida.')
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  it('al llegar desde la presentación con fechas y grupo en la dirección, abre ya filtrado', async () => {
    const [entrada, salida] = [fechaDesdeHoy(5), fechaDesdeHoy(7)]
    const fetch = laApiResponde([200, pagina(SUITE_PARA_EL_GRUPO)])
    render(
      <MemoryRouter initialEntries={[`/apartamentos?entrada=${entrada}&salida=${salida}&adultos=3&ninos=1`]}>
        <Apartamentos alojamiento={alojamiento(12)} />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '1 apartamento libre' })).toBeInTheDocument()
    expect(urlDeLaLlamada(fetch, 0))
      .toBe(`/api/portal/apartamentos/disponibles?entrada=${entrada}&salida=${salida}&ocupantes=4&ninos=1&pagina=0`)
    expect(fetch).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: /4 personas/ })).toBeInTheDocument()
  })

  it('con fechas que no sirven en la dirección, muestra todos los apartamentos', async () => {
    const fetch = laApiResponde([200, pagina(SUITE)])
    render(
      <MemoryRouter initialEntries={[`/apartamentos?entrada=2020-01-01&salida=2020-01-03&adultos=2&ninos=0`]}>
        <Apartamentos alojamiento={alojamiento(12)} />
      </MemoryRouter>,
    )

    await screen.findByRole('article', { name: 'Suite Jardín' })
    expect(urlDeLaLlamada(fetch, 0)).toBe('/api/portal/apartamentos?pagina=0')
  })
})
