import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { OcupacionApartamento } from '../api/ocupacion'
import { fechaHoy, sumarDias } from '../formato'
import { paginaDe, reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta, llamada } from '../pruebas/sesionDePrueba'
import type { Rol } from '../sesion/roles'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RANGO = '/ocupacion?desde=2026-11-12&ultima=2026-11-25'

const FILA: OcupacionApartamento = {
  apartamento: 'APT-101',
  nombre: 'Penthouse Mirador',
  estadoOperativo: 'OCUPADO',
  reservas: [{
    codigo: 'RES-2026-00042', entrada: '2026-11-14', salida: '2026-11-16', estado: 'CONFIRMADA', canalOrigen: 'DIRECTO', titularId: 'TIT-7',
  }],
  bloqueos: [{ id: 'BLO-1', fechaInicio: '2026-11-18', fechaFin: '2026-11-21', motivo: 'Mantenimiento: pintura' }],
}
const VACIO: OcupacionApartamento = { apartamento: 'APT-205', nombre: 'Loft', estadoOperativo: 'PREPARADO', reservas: [], bloqueos: [] }

function abrir(direccion = RANGO, rol: Rol = 'RECEPCIONISTA') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

function api(filas: OcupacionApartamento[] = [FILA, VACIO]) {
  return laApiRespondePorRuta({
    '/api/ocupacion': [200, paginaDe(filas)],
    '/api/apartamentos': [200, paginaDe([{ codigo: 'APT-101' }, { codigo: 'APT-205' }])],
    '/api/alojamiento': [200, { nombre: 'Puerta al Sol' }],
    '/api/reservas/RES-2026-00042': [200, reservaDePrueba()],
  })
}

function consultasDeOcupacion(fetch: ReturnType<typeof api>) {
  return fetch.mock.calls.map((_, n) => llamada(fetch, n).url).filter((url) => url.startsWith('/api/ocupacion'))
}

describe('tablero de ocupación (DEC-86)', () => {
  it('aparece en el menú de recepción, entre las reservas y los titulares', async () => {
    api()
    abrir()

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' })
    expect(within(menu).getByRole('link', { name: 'Ocupación' })).toHaveAttribute('href', '/ocupacion')
  })

  it('el personal de servicio no puede abrirlo', () => {
    api()
    abrir(RANGO, 'PERSONAL_SERVICIO')

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('por defecto muestra 14 noches desde hoy', async () => {
    const fetch = api()
    abrir('/ocupacion')

    await screen.findByRole('group', { name: 'Ocupación por noche' })
    const hoy = fechaHoy()
    expect(consultasDeOcupacion(fetch)[0]).toContain(`desde=${hoy}&hasta=${sumarDias(hoy, 14)}`)
    expect(screen.getByLabelText('Última noche')).toHaveValue(sumarDias(hoy, 13))
  })

  it('pide un día más que la última noche, porque `hasta` es exclusiva (EST-01)', async () => {
    const fetch = api()
    abrir()

    await screen.findByRole('group', { name: 'Ocupación por noche' })
    expect(consultasDeOcupacion(fetch)[0]).toContain('desde=2026-11-12&hasta=2026-11-26')
    expect(screen.getByText('12 nov 2026 → 26 nov 2026 · 14 noches')).toBeInTheDocument()
  })

  it('muestra cada apartamento con su estado en palabras', async () => {
    api()
    abrir()

    const cuadricula = await screen.findByRole('group', { name: 'Ocupación por noche' })
    expect(within(cuadricula).getByText('Penthouse Mirador')).toBeInTheDocument()
    expect(within(cuadricula).getByText('Ocupado')).toBeInTheDocument()
    expect(within(cuadricula).getByText('Preparado')).toBeInTheDocument()
  })

  it('la barra de una reserva dice qué es y lleva a su detalle', async () => {
    api()
    abrir()

    const cuadricula = await screen.findByRole('group', { name: 'Ocupación por noche' })
    const barra = within(cuadricula).getByRole('link', {
      name: 'Reserva RES-2026-00042, Confirmada, titular TIT-7, del 14 nov 2026 al 16 nov 2026',
    })
    expect(barra).toHaveAttribute('href', '/reservas/RES-2026-00042')
    expect(barra).toHaveStyle({ gridColumn: '3 / span 2' })
  })

  it('un bloqueo se ve distinto, con su motivo, y no lleva a ningún lado', async () => {
    api()
    abrir()

    const cuadricula = await screen.findByRole('group', { name: 'Ocupación por noche' })
    const bloqueo = within(cuadricula).getByRole('img', { name: /^Bloqueo: Mantenimiento: pintura/ })
    expect(bloqueo).toHaveStyle({ gridColumn: '7 / span 3' })
    expect(within(cuadricula).getAllByRole('link')).toHaveLength(1)
  })

  it('un apartamento vacío también aparece y la lista lo dice', async () => {
    api()
    abrir()

    expect(await screen.findByText('Sin reservas ni bloqueos en este rango.')).toBeInTheDocument()
  })

  it('anterior y siguiente mueven el rango completo', async () => {
    const fetch = api()
    abrir()
    const usuario = userEvent.setup()
    await screen.findByRole('group', { name: 'Ocupación por noche' })

    await usuario.click(screen.getAllByRole('button', { name: /Siguiente/ })[0])
    await screen.findByText('26 nov 2026 → 10 dic 2026 · 14 noches')
    expect(consultasDeOcupacion(fetch).at(-1)).toContain('desde=2026-11-26&hasta=2026-12-10')

    await usuario.click(screen.getAllByRole('button', { name: /Anterior/ })[0])
    await screen.findByText('12 nov 2026 → 26 nov 2026 · 14 noches')
  })

  it('«Hoy» vuelve a hoy conservando cuántas noches se ven', async () => {
    const fetch = api()
    abrir('/ocupacion?desde=2020-01-01&ultima=2020-01-07')
    const usuario = userEvent.setup()
    await screen.findByRole('group', { name: 'Ocupación por noche' })

    await usuario.click(screen.getByRole('button', { name: 'Hoy' }))

    const hoy = fechaHoy()
    await screen.findByText(new RegExp('7 noches'))
    expect(consultasDeOcupacion(fetch).at(-1)).toContain(`desde=${hoy}&hasta=${sumarDias(hoy, 7)}`)
  })

  it('filtra por apartamento', async () => {
    const fetch = api([FILA])
    abrir()
    const usuario = userEvent.setup()
    await screen.findByRole('option', { name: 'APT-205' })

    await usuario.selectOptions(screen.getByLabelText('Apartamento'), 'APT-101')

    await screen.findByRole('group', { name: 'Ocupación por noche' })
    expect(consultasDeOcupacion(fetch).at(-1)).toContain('apartamento=APT-101')
  })

  it('una última noche anterior a «Desde» se marca y no consulta la API', async () => {
    const fetch = api()
    abrir()
    await screen.findByRole('group', { name: 'Ocupación por noche' })
    const antes = consultasDeOcupacion(fetch).length

    fireEvent.change(screen.getByLabelText('Última noche'), { target: { value: '2026-11-10' } })

    expect(await screen.findByText('La última noche no puede ser anterior a «Desde».')).toBeInTheDocument()
    expect(screen.getByLabelText('Última noche')).toBeInvalid()
    expect(screen.getByRole('alert')).toHaveTextContent('Rango inválido')
    expect(consultasDeOcupacion(fetch)).toHaveLength(antes)
  })

  it('una última noche igual a «Desde» es válida: es una noche', async () => {
    api()
    abrir('/ocupacion?desde=2026-11-12&ultima=2026-11-12')

    expect(await screen.findByText('12 nov 2026 → 13 nov 2026 · 1 noche')).toBeInTheDocument()
  })

  it('sin apartamentos lo explica', async () => {
    api([])
    abrir()

    expect(await screen.findByRole('heading', { name: 'Aún no hay apartamentos' })).toBeInTheDocument()
  })

  it('si falla se puede reintentar', async () => {
    let falla = true
    laApiRespondePorRuta({
      '/api/alojamiento': [200, { nombre: 'Puerta al Sol' }],
      '/api/apartamentos': [200, paginaDe([])],
      '/api/ocupacion': () => (falla
        ? [500, { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'Algo falló', detalles: [] }]
        : [200, paginaDe([FILA])]),
    })
    abrir()
    const usuario = userEvent.setup()

    const error = await screen.findByRole('alert')
    expect(error).toHaveTextContent('No pudimos cargar la ocupación')
    falla = false
    await usuario.click(within(error).getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('group', { name: 'Ocupación por noche' })).toBeInTheDocument()
  })

  it('un estado operativo desconocido se muestra tal cual, sin romper', async () => {
    api([{ ...VACIO, estadoOperativo: 'NUEVO_ESTADO' }])
    abrir()

    const cuadricula = await screen.findByRole('group', { name: 'Ocupación por noche' })
    expect(within(cuadricula).getByText('NUEVO_ESTADO')).toBeInTheDocument()
  })
})
