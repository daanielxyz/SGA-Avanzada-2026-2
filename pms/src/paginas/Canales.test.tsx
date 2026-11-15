import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Canal, Conflicto } from '../api/canales'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const PORTAL: Canal = { id: 'CAN-1', nombre: 'Portal web', tipo: 'PORTAL', referenciaCredencial: null, activo: true }
const RECEPCION: Canal = { id: 'CAN-2', nombre: 'Recepción', tipo: 'DIRECTO', referenciaCredencial: null, activo: true }
const BOOKING: Canal = { id: 'CAN-3', nombre: 'Booking', tipo: 'EXTERNO', referenciaCredencial: 'BOOKING_API_KEY', activo: true }
const AIRBNB: Canal = { id: 'CAN-4', nombre: 'Airbnb', tipo: 'EXTERNO', referenciaCredencial: 'AIRBNB_API_KEY', activo: false }
const CANALES = [PORTAL, RECEPCION, BOOKING, AIRBNB]

function conflicto(id: string, idExterno: string, cambios: Partial<Conflicto> = {}): Conflicto {
  return {
    id, canalId: 'CAN-3', idExterno, apartamento: 'APT-103', entrada: '2026-11-20', salida: '2026-11-23',
    detectadoEn: '2026-11-16T21:12:00', motivo: 'El apartamento APT-103 ya está reservado en esas fechas (RES-2026-00042)',
    titularNombre: 'Eva Mora', titularTelefono: '310 555 0142', titularCorreo: null,
    estado: 'PENDIENTE', resueltoPor: null, resueltoEn: null, decision: null, ...cambios,
  }
}
const CHOQUE = conflicto('CON-1', 'BK-558120')
const SIN_CONTACTO = conflicto('CON-2', 'BK-558377', { titularNombre: null, titularTelefono: null })

const error = (estado: number, codigo: string, mensaje: string, detalles: string[] = []): [number, unknown] =>
  [estado, { estado, codigo, mensaje, detalles }]

function api(rutas: Parameters<typeof laApiRespondePorRuta>[0] = {}) {
  return laApiRespondePorRuta({
    '/api/canales': [200, paginaDe(CANALES)],
    '/api/conflictos/pendientes': [200, paginaDe([CHOQUE, SIN_CONTACTO])],
    ...rutas,
  })
}

function abrir(rol: Rol = 'ADMINISTRADOR') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={['/canales']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

const tabla = async () => screen.findByRole('table', { name: 'Canales de venta' })
const fila = async (nombre: string) => within(await tabla()).getByRole('row', { name: new RegExp(nombre) })
const envios = (fetch: ReturnType<typeof api>, metodo: string) => fetch.mock.calls
  .filter(([, opciones]) => (opciones as RequestInit | undefined)?.method === metodo)
  .map(([url, opciones]) => ({ url, cuerpo: (opciones as RequestInit).body ? JSON.parse(String((opciones as RequestInit).body)) : undefined }))

describe('canales de venta', () => {
  it('muestra los conflictos con a quién llamar y los canales con sus textos, sin valores crudos de la API', async () => {
    api()
    abrir()
    expect(await screen.findByRole('heading', { name: 'Conflictos pendientes · 2' })).toBeInTheDocument()
    const llamar = screen.getByRole('link', { name: 'Llamar a Eva Mora al 310 555 0142' })
    expect(llamar).toHaveAttribute('href', 'tel:3105550142')
    expect(screen.getAllByText('Booking', { exact: false }).length).toBeGreaterThan(0)
    expect(screen.getAllByText('20 nov 2026 → 23 nov 2026 · 3 noches')).toHaveLength(2)
    expect(screen.getByText('Sin datos de contacto')).toBeInTheDocument()

    const booking = await fila('Booking')
    expect(booking).toHaveTextContent('Externo')
    expect(booking).toHaveTextContent('BOOKING_API_KEY')
    expect(booking).toHaveTextContent('Activo')
    expect(within(booking).getByRole('link', { name: 'Ver bitácora de Booking' })).toHaveAttribute('href', '/canales/CAN-3/bitacora')
    expect(within(booking).getByRole('button', { name: 'Desactivar Booking' })).toBeInTheDocument()
    expect(document.body).not.toHaveTextContent('EXTERNO')
  })

  it('el portal y recepción no se desactivan; el externo inactivo solo tiene la bitácora', async () => {
    api()
    abrir()
    for (const nombre of ['Portal web', 'Recepción']) {
      const canal = await fila(nombre)
      expect(within(canal).queryByRole('button', { name: /Desactivar/ })).not.toBeInTheDocument()
      expect(within(canal).queryByRole('link', { name: /bitácora/ })).not.toBeInTheDocument()
    }
    const airbnb = await fila('Airbnb')
    expect(airbnb).toHaveTextContent('Inactivo')
    expect(within(airbnb).queryByRole('button', { name: /Desactivar/ })).not.toBeInTheDocument()
    expect(within(airbnb).getByRole('link', { name: 'Ver bitácora de Airbnb' })).toBeInTheDocument()
  })

  it('sin conflictos lo dice, y si falla un bloque el otro se sigue viendo', async () => {
    api({ '/api/conflictos/pendientes': error(500, 'ERROR_INTERNO', 'Falló el servidor') })
    abrir()
    expect(await screen.findByText('No pudimos cargar los conflictos', { exact: false })).toBeInTheDocument()
    expect(await fila('Booking')).toBeInTheDocument()
  })

  it('dice cuando no hay conflictos pendientes', async () => {
    api({ '/api/conflictos/pendientes': [200, paginaDe([])] })
    abrir()
    expect(await screen.findByText(/No hay conflictos pendientes/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Conflictos pendientes' })).toBeInTheDocument()
  })

  it('registra un canal externo con la referencia de la clave y uno directo sin ella', async () => {
    const fetch = api({ '/api/canales': (metodo) => (metodo === 'POST' ? [201, BOOKING] : [200, paginaDe(CANALES)]) })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Registrar canal/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar canal' })

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar canal' }))
    expect(within(dialogo).getByText('Escribe el nombre del canal.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Elige el tipo.')).toBeInTheDocument()
    expect(within(dialogo).queryByLabelText('Referencia de la clave')).not.toBeInTheDocument()

    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Booking')
    await usuario.click(within(dialogo).getByRole('radio', { name: /Externo/ }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar canal' }))
    expect(within(dialogo).getByText('Escribe la referencia de la clave.')).toBeInTheDocument()
    expect(within(dialogo).getByText(/no la clave/)).toBeInTheDocument()

    await usuario.type(within(dialogo).getByLabelText('Referencia de la clave'), 'BOOKING_API_KEY')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar canal' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(envios(fetch, 'POST')).toEqual([{ url: '/api/canales', cuerpo: { nombre: 'Booking', tipo: 'EXTERNO', referenciaCredencial: 'BOOKING_API_KEY' } }])
    expect(await screen.findByText('Booking quedó activo.')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: /Registrar canal/ }))
    const otro = await screen.findByRole('dialog', { name: 'Registrar canal' })
    await usuario.type(within(otro).getByLabelText('Nombre'), 'WhatsApp')
    await usuario.click(within(otro).getByRole('radio', { name: /Directo/ }))
    await usuario.click(within(otro).getByRole('button', { name: 'Registrar canal' }))
    await waitFor(() => expect(envios(fetch, 'POST')).toHaveLength(2))
    expect(envios(fetch, 'POST')[1].cuerpo).toEqual({ nombre: 'WhatsApp', tipo: 'DIRECTO' })
  })

  it('un 400 del registro sale junto al campo', async () => {
    api({ '/api/canales': (metodo) => (metodo === 'POST'
      ? error(400, 'DATOS_INVALIDOS', 'Datos inválidos', ['nombre: admite máximo 80 caracteres'])
      : [200, paginaDe(CANALES)]) })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Registrar canal/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar canal' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Booking')
    await usuario.click(within(dialogo).getByRole('radio', { name: /Portal web/ }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar canal' }))
    expect(await within(dialogo).findByText('Admite máximo 80 caracteres.')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Nombre')).toHaveAttribute('aria-invalid', 'true')
  })

  it('desactiva un canal externo, y si ya estaba inactivo recarga la lista y lo dice', async () => {
    const fetch = api({ '/api/canales/CAN-3/desactivar': [200, { ...BOOKING, activo: false }] })
    const usuario = abrir()
    await usuario.click(within(await fila('Booking')).getByRole('button', { name: 'Desactivar Booking' }))
    const dialogo = await screen.findByRole('dialog', { name: '¿Desactivar Booking?' })
    expect(dialogo).toHaveTextContent('no se puede deshacer desde el PMS')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Desactivar canal' }))
    expect(await screen.findByText('Booking quedó inactivo.')).toBeInTheDocument()
    expect(envios(fetch, 'PUT')).toEqual([{ url: '/api/canales/CAN-3/desactivar', cuerpo: undefined }])
  })

  it('si otra persona ya desactivó el canal, cierra el diálogo y recarga', async () => {
    const fetch = api({ '/api/canales/CAN-3/desactivar': error(409, 'REGLA_NEGOCIO', 'El canal CAN-3 ya está inactivo') })
    const usuario = abrir()
    await usuario.click(within(await fila('Booking')).getByRole('button', { name: 'Desactivar Booking' }))
    await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Desactivar canal' }))
    expect(await screen.findByText('Otra persona desactivó Booking. La lista quedó al día.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(fetch.mock.calls.filter(([url]) => String(url).startsWith('/api/canales?')).length).toBeGreaterThan(1)
  })

  it('resuelve un conflicto con la decisión, que es obligatoria', async () => {
    const fetch = api({ '/api/conflictos/CON-1/resolver': [200, { ...CHOQUE, estado: 'RESUELTO', decision: 'Lo pasamos al APT-104' }] })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Resolver el conflicto de BK-558120' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Resolver conflicto' })
    expect(dialogo).toHaveTextContent('Booking')
    expect(dialogo).toHaveTextContent('Esto no cambia ninguna reserva')
    expect(within(dialogo).getByRole('link', { name: 'Llamar a Eva Mora al 310 555 0142' })).toBeInTheDocument()

    await usuario.click(within(dialogo).getByRole('button', { name: 'Marcar como resuelto' }))
    expect(within(dialogo).getByText('Escribe qué se decidió.')).toBeInTheDocument()
    await usuario.type(within(dialogo).getByLabelText('¿Qué se decidió?'), '  Lo pasamos al APT-104  ')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Marcar como resuelto' }))
    expect(await screen.findByText(/Quedó anotado qué se decidió con la reserva externa BK-558120/)).toBeInTheDocument()
    expect(envios(fetch, 'PUT')).toEqual([{ url: '/api/conflictos/CON-1/resolver', cuerpo: { decision: 'Lo pasamos al APT-104' } }])
  })

  it('si el conflicto ya lo resolvió otra persona, cierra el diálogo y lo dice', async () => {
    api({ '/api/conflictos/CON-1/resolver': error(409, 'REGLA_NEGOCIO', 'El conflicto CON-1 ya está resuelto') })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Resolver el conflicto de BK-558120' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Resolver conflicto' })
    await usuario.type(within(dialogo).getByLabelText('¿Qué se decidió?'), 'Reubicado')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Marcar como resuelto' }))
    expect(await screen.findByText(/Este conflicto ya lo resolvió otra persona/)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('recepción no entra: es solo de administración', async () => {
    api()
    abrir('RECEPCIONISTA')
    expect(await screen.findByRole('heading', { level: 1 })).not.toHaveTextContent('Canales de venta')
  })
})
