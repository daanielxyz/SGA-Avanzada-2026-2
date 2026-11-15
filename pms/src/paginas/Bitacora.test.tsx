import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Canal, EventoCanal } from '../api/canales'
import type { Pagina } from '../api/reservas'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const BOOKING: Canal = { id: 'CAN-3', nombre: 'Booking', tipo: 'EXTERNO', referenciaCredencial: 'BOOKING_API_KEY', activo: true }
const AIRBNB: Canal = { id: 'CAN-4', nombre: 'Airbnb', tipo: 'EXTERNO', referenciaCredencial: 'AIRBNB_API_KEY', activo: false }

const evento = (id: string, cambios: Partial<EventoCanal>): EventoCanal => ({
  id, operacion: 'CREAR_RESERVA', sentido: 'ENTRANTE', fechaHora: '2026-11-17T07:05:00',
  cargaUtil: '{"idExterno":"BK-558377","titular":{"nombre":"Eva Mora"}}', resultado: 'EXITOSO', detalle: 'RES-2026-00051',
  ...cambios,
})
const EVENTOS = [
  evento('EVT-1', { resultado: 'CONFLICTO', detalle: 'El apartamento APT-108 admite máximo 2 ocupantes' }),
  evento('EVT-2', { operacion: 'CONSULTAR_DISPONIBILIDAD', fechaHora: '2026-11-16T21:11:00', detalle: '6 apartamentos disponibles' }),
  evento('EVT-3', { resultado: 'DUPLICADO', fechaHora: '2026-11-13T11:48:00' }),
  evento('EVT-4', { operacion: 'PUBLICAR_DISPONIBILIDAD', sentido: 'SALIENTE', resultado: 'ERROR', detalle: null, cargaUtil: 'texto plano' }),
]

const pagina = (contenido: EventoCanal[], cambios: Partial<Pagina<EventoCanal>> = {}) => ({ ...paginaDe(contenido), ...cambios })

function abrir(id = 'CAN-3', rutas: Parameters<typeof laApiRespondePorRuta>[0] = {}) {
  const fetch = laApiRespondePorRuta({
    '/api/canales': [200, paginaDe([BOOKING, AIRBNB])],
    [`/api/canales/${id}/eventos`]: [200, pagina(EVENTOS, { totalElementos: 14, totalPaginas: 2 })],
    ...rutas,
  })
  entrarComo('ADMINISTRADOR')
  render(
    <MemoryRouter initialEntries={[`/canales/${id}/bitacora`]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return { fetch, usuario: userEvent.setup() }
}

const consultas = (fetch: ReturnType<typeof laApiRespondePorRuta>) => fetch.mock.calls
  .map(([url]) => String(url)).filter((url) => url.includes('/eventos?'))

describe('bitácora de un canal', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 17, 10, 40))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('abre con los últimos 7 días y muestra cada mensaje con sus textos, sin valores crudos', async () => {
    const { fetch } = abrir()
    expect(await screen.findByRole('heading', { level: 1, name: 'Bitácora de Booking' })).toBeInTheDocument()
    expect(consultas(fetch)[0]).toBe('/api/canales/CAN-3/eventos?desde=2026-11-11&hasta=2026-11-17&pagina=0')
    expect(await screen.findByText('14 mensajes del 11 nov 2026 al 17 nov 2026, del más reciente al más antiguo.')).toBeInTheDocument()

    const lista = screen.getByRole('list', { name: 'Mensajes con Booking' })
    const [primero, segundo, tercero, cuarto] = within(lista).getAllByRole('listitem')
    expect(primero).toHaveTextContent('Envió una reserva')
    expect(primero).toHaveTextContent('Recibido del canal')
    expect(primero).toHaveTextContent('Rechazado por disponibilidad')
    expect(primero).toHaveTextContent('7:05 a. m.')
    expect(segundo).toHaveTextContent('Consultó disponibilidad')
    expect(segundo).toHaveTextContent('Hecho')
    expect(tercero).toHaveTextContent('Repetido')
    expect(cuarto).toHaveTextContent('Le enviamos la disponibilidad')
    expect(cuarto).toHaveTextContent('Enviado al canal')
    expect(cuarto).toHaveTextContent('Error')
    expect(document.body).not.toHaveTextContent(/CREAR_RESERVA|ENTRANTE|CONFLICTO|EXITOSO/)
  })

  it('el mensaje del canal va plegado, como texto y con sangría', async () => {
    const { usuario } = abrir()
    const lista = await screen.findByRole('list', { name: 'Mensajes con Booking' })
    const primero = within(lista).getAllByRole('listitem')[0]
    const detalles = primero.querySelector('details')
    expect(detalles).not.toHaveAttribute('open')
    await usuario.click(within(primero).getByText('Ver mensaje del canal'))
    expect(detalles).toHaveAttribute('open')
    expect(primero.querySelector('pre')?.textContent).toContain('"titular": {')
  })

  it('no consulta si «Hasta» es antes de «Desde», y consulta las fechas elegidas desde la primera página', async () => {
    const { fetch, usuario } = abrir()
    await screen.findByRole('list', { name: 'Mensajes con Booking' })
    const desde = screen.getByLabelText('Desde')
    const hasta = screen.getByLabelText('Hasta')

    await usuario.clear(hasta)
    await usuario.type(hasta, '2026-11-01')
    await usuario.click(screen.getByRole('button', { name: 'Consultar' }))
    expect(screen.getByText('«Hasta» no puede ser antes de «Desde».')).toBeInTheDocument()
    expect(hasta).toHaveAttribute('aria-invalid', 'true')
    expect(consultas(fetch)).toHaveLength(1)

    await usuario.clear(desde)
    await usuario.type(desde, '2026-10-01')
    await usuario.click(screen.getByRole('button', { name: 'Consultar' }))
    await waitFor(() => expect(consultas(fetch)).toHaveLength(2))
    expect(consultas(fetch)[1]).toBe('/api/canales/CAN-3/eventos?desde=2026-10-01&hasta=2026-11-01&pagina=0')
  })

  it('pasa de página', async () => {
    const { fetch, usuario } = abrir()
    await screen.findByRole('list', { name: 'Mensajes con Booking' })
    await usuario.click(screen.getByRole('button', { name: /Siguiente/ }))
    await waitFor(() => expect(consultas(fetch)).toHaveLength(2))
    expect(consultas(fetch)[1]).toContain('pagina=1')
    // La lista se desmonta mientras carga: el foco vuelve al resumen, no se pierde en la página
    await waitFor(() => expect(document.activeElement).toHaveClass('bitacora-resumen'))
  })

  it('sin mensajes en esas fechas lo dice', async () => {
    abrir('CAN-3', { '/api/canales/CAN-3/eventos': [200, pagina([])] })
    expect(await screen.findByRole('heading', { name: 'Sin mensajes en esas fechas' })).toBeInTheDocument()
  })

  it('un canal inactivo lo avisa, y un canal que no existe da la página de no encontrado', async () => {
    abrir('CAN-4', { '/api/canales/CAN-4/eventos': [200, pagina([])] })
    expect(await screen.findByText(/Airbnb está inactivo: no crea reservas nuevas/)).toBeInTheDocument()
  })

  it('un canal que no existe (404) lo dice y deja volver', async () => {
    abrir('CAN-99', { '/api/canales/CAN-99/eventos': [404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe el canal', detalles: [] }] })
    expect(await screen.findByRole('heading', { level: 1, name: 'Este canal no existe' })).toBeInTheDocument()
    const volver = document.querySelector('.bitacora .volver')
    expect(volver).toHaveTextContent('Canales de venta')
    expect(volver).toHaveAttribute('href', '/canales')
  })

  it('un error al cargar ofrece reintentar', async () => {
    const { fetch, usuario } = abrir('CAN-3', { '/api/canales/CAN-3/eventos': [500, { estado: 500, codigo: 'ERROR', mensaje: 'Falló', detalles: [] }] })
    await usuario.click(await screen.findByRole('button', { name: 'Reintentar' }))
    await waitFor(() => expect(consultas(fetch)).toHaveLength(2))
  })
})
