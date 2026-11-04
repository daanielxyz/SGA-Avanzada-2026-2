import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Reserva } from '../api/reservas'
import { paginaDe, reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

function abrir(direccion = '/reservas') {
  entrarComo('RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

function consultasAlListado(fetch: ReturnType<typeof laApiRespondePorRuta>) {
  return fetch.mock.calls.map(([url]) => url).filter((url) => url.startsWith('/api/reservas'))
}

describe('listado de reservas (CU-25)', () => {
  it('muestra cada reserva con el nombre del titular, sus fechas, su estado y su total', async () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([reservaDePrueba()])] })
    abrir()

    const tabla = await screen.findByRole('table', { name: 'Reservas' })
    const fila = within(tabla).getByRole('row', { name: /RES-2026-00042/ })
    expect(within(fila).getByRole('link', { name: 'RES-2026-00042' })).toHaveAttribute('href', '/reservas/RES-2026-00042')
    expect(within(fila).getByText('Ana Ríos')).toBeInTheDocument()
    expect(within(fila).getByText('TIT-7')).toBeInTheDocument()
    expect(within(fila).getByText('14 nov 2026')).toBeInTheDocument()
    expect(within(fila).getByText('No se presentó')).toBeInTheDocument()
    expect(within(fila).getByText('$ 1.250.000')).toBeInTheDocument()
    expect(within(fila).getByText('Portal')).toBeInTheDocument()
  })

  it('sin nombre del titular muestra su id, y un estado desconocido no rompe la pantalla', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [200, paginaDe([
        reservaDePrueba({ titularNombre: null, estado: 'ESTADO_NUEVO' as Reserva['estado'] }),
      ])],
    })
    abrir()

    const tabla = await screen.findByRole('table', { name: 'Reservas' })
    expect(within(tabla).getByText('TIT-7')).toBeInTheDocument()
    expect(within(tabla).getByText('ESTADO_NUEVO')).toBeInTheDocument()
  })

  it('los datos extremos se muestran completos y como texto', async () => {
    const nombre = 'María Constanza Wiśniewska-Kowalczyk Rodríguez de la Hoz y Fernández-Villamizar Oberhauser'
    laApiRespondePorRuta({
      '/api/reservas': [200, paginaDe([
        reservaDePrueba({
          titularNombre: nombre,
          apartamento: 'APT-SUITE-PRESIDENCIAL-TORRE-NORTE-1204',
          valor: { desglose: [], total: 999999999 },
        }),
        reservaDePrueba({ codigo: 'RES-2026-00043', titularNombre: '<script>alert(1)</script>' }),
      ])],
    })
    abrir()

    const tabla = await screen.findByRole('table', { name: 'Reservas' })
    expect(within(tabla).getByText('$ 999.999.999')).toBeInTheDocument()
    // Lo que la tabla recorta se puede leer completo al pasar el cursor
    expect(within(tabla).getByTitle(nombre)).toBeInTheDocument()
    expect(within(tabla).getByTitle('APT-SUITE-PRESIDENCIAL-TORRE-NORTE-1204')).toBeInTheDocument()
    expect(within(tabla).getByText('<script>alert(1)</script>')).toBeInTheDocument()
  })

  it('mientras carga lo dice', () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([])] })
    abrir()

    expect(screen.getByRole('status')).toHaveTextContent('Cargando reservas…')
  })

  it('sin reservas y sin filtros explica que todavía no hay', async () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([])] })
    abrir()

    expect(await screen.findByRole('heading', { name: 'Todavía no hay reservas' })).toBeInTheDocument()
  })

  it('sin resultados con filtros ofrece limpiarlos', async () => {
    const fetch = laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([])] })
    abrir('/reservas?estado=CANCELADA')

    expect(await screen.findByRole('heading', { name: 'Ninguna reserva coincide con los filtros' })).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Limpiar filtros' }))

    await screen.findByRole('heading', { name: 'Todavía no hay reservas' })
    expect(consultasAlListado(fetch)).toEqual(['/api/reservas?estado=CANCELADA', '/api/reservas'])
  })

  it('filtrar consulta la API con lo elegido y deja el filtro a la vista', async () => {
    const fetch = laApiRespondePorRuta({
      '/api/reservas': [200, paginaDe([reservaDePrueba()])],
      '/api/apartamentos': [200, paginaDe([{ codigo: 'APT-101' }, { codigo: 'APT-102' }])],
    })
    abrir()
    const usuario = userEvent.setup()
    const filtros = screen.getByRole('search', { name: 'Filtros' })
    await within(filtros).findByRole('option', { name: 'APT-102' })

    await usuario.selectOptions(within(filtros).getByLabelText('Estado'), 'Confirmada')
    await usuario.selectOptions(within(filtros).getByLabelText('Apartamento'), 'APT-102')
    await usuario.click(within(filtros).getByRole('button', { name: 'Filtrar' }))

    await waitFor(() =>
      expect(consultasAlListado(fetch)).toContain('/api/reservas?estado=CONFIRMADA&apartamento=APT-102'),
    )
    expect(screen.getByRole('button', { name: 'Quitar filtro Estado: Confirmada' })).toBeInTheDocument()
  })

  it('quitar un filtro deja los demás', async () => {
    const fetch = laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([reservaDePrueba()])] })
    abrir('/reservas?estado=CONFIRMADA&canal=PORTAL')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Quitar filtro Estado: Confirmada' }))

    await waitFor(() => expect(consultasAlListado(fetch)).toContain('/api/reservas?canal=PORTAL'))
  })

  it('un rango de fechas al revés muestra el mensaje del backend junto a los filtros (409)', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [409, {
        estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La fecha desde debe ser anterior a la fecha hasta', detalles: [],
      }],
    })
    abrir('/reservas?desde=2026-11-20&hasta=2026-11-14')

    const filtros = screen.getByRole('search', { name: 'Filtros' })
    expect(await within(filtros).findByRole('alert')).toHaveTextContent('La fecha desde debe ser anterior a la fecha hasta')
    expect(within(filtros).getByLabelText('Desde')).toBeInvalid()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    // En el celular los filtros van plegados: el error los abre para que se vea
    expect(screen.getByRole('button', { name: /^Filtros/ })).toHaveAttribute('aria-expanded', 'true')
  })

  it('una fecha inválida en la dirección no muestra basura en el filtro aplicado', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [400, { estado: 400, codigo: 'VALIDACION', mensaje: 'Datos inválidos', detalles: [] }],
    })
    abrir('/reservas?desde=xxx')

    expect(await screen.findByRole('button', { name: 'Quitar filtro Desde: xxx' })).toBeInTheDocument()
  })

  it('un filtro con formato inválido muestra los detalles del 400', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [400, {
        estado: 400,
        codigo: 'VALIDACION',
        mensaje: 'La petición no es válida',
        detalles: ['canal: debe ser PORTAL, DIRECTO o EXTERNO'],
      }],
    })
    abrir('/reservas?canal=OTRO')

    expect(await screen.findByRole('alert')).toHaveTextContent('canal: debe ser PORTAL, DIRECTO o EXTERNO')
  })

  it('si el servidor no responde se puede reintentar', async () => {
    let fallar = true
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.startsWith('/api/reservas') && fallar) return Promise.reject(new TypeError('sin red'))
      return Promise.resolve(new Response(JSON.stringify(paginaDe([reservaDePrueba()])), { status: 200 }))
    }))
    abrir()

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos cargar las reservas')
    fallar = false
    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('table', { name: 'Reservas' })).toBeInTheDocument()
  })

  it('el paginador avanza con la API y se detiene en los extremos', async () => {
    const fetch = laApiRespondePorRuta({
      '/api/reservas': [200, { ...paginaDe([reservaDePrueba()]), numero: 0, totalPaginas: 7, totalElementos: 64 }],
    })
    abrir()

    const paginacion = await screen.findByRole('navigation', { name: 'Paginación' })
    expect(paginacion).toHaveTextContent('Página 1 de 7 · 64 reservas')
    expect(within(paginacion).getByRole('button', { name: 'Anterior' })).toBeDisabled()
    await userEvent.setup().click(within(paginacion).getByRole('button', { name: 'Siguiente' }))

    await waitFor(() => expect(consultasAlListado(fetch)).toContain('/api/reservas?pagina=1'))
  })

  it('una sola reserva se dice en singular', async () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([reservaDePrueba()])] })
    abrir()

    expect(await screen.findByRole('navigation', { name: 'Paginación' })).toHaveTextContent('1 reserva')
  })

  it('«Ir a la reserva» abre el detalle aunque el código se escriba en minúsculas', async () => {
    laApiRespondePorRuta({
      '/api/reservas': [200, paginaDe([])],
      '/api/reservas/RES-2026-00042': [200, reservaDePrueba()],
    })
    abrir()
    const usuario = userEvent.setup()

    await usuario.type(screen.getByLabelText('Ir a la reserva'), 'res-2026-00042')
    await usuario.click(screen.getByRole('button', { name: 'Ir' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'RES-2026-00042' })).toBeInTheDocument()
  })

  it('«Ir a la reserva» sin código avisa sin salir del listado', async () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([])] })
    abrir()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Ir' }))

    expect(screen.getByLabelText('Ir a la reserva')).toHaveAccessibleDescription('Escribe el código de la reserva.')
    expect(screen.getByRole('heading', { level: 1, name: 'Reservas' })).toBeInTheDocument()
  })

  it('el personal de servicio no puede abrir el listado', () => {
    laApiRespondePorRuta({})
    entrarComo('PERSONAL_SERVICIO')
    render(
      <MemoryRouter initialEntries={['/reservas']}>
        <ProveedorSesion>
          <App />
        </ProveedorSesion>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })
})
