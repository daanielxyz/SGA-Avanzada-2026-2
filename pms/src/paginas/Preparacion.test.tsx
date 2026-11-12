import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { ApartamentoOperativo } from '../api/apartamentos'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const APARTAMENTOS: ApartamentoOperativo[] = [
  { codigo: 'APT-101', nombre: 'Penthouse Mirador', estadoOperativo: 'PENDIENTE_PREPARACION' },
  { codigo: 'APT-102', nombre: 'Apartamento Jardín', estadoOperativo: 'PREPARADO' },
  { codigo: 'APT-103', nombre: 'Estudio Terraza', estadoOperativo: 'EN_PREPARACION' },
  { codigo: 'APT-104', nombre: 'Loft Central', estadoOperativo: 'OCUPADO' },
  { codigo: 'APT-110', nombre: 'Mirador del Lago', estadoOperativo: 'FUERA_DE_SERVICIO' },
]

const DESTINO: Record<string, string> = {
  'iniciar-preparacion': 'EN_PREPARACION',
  'marcar-preparado': 'PREPARADO',
  'declarar-fuera-de-servicio': 'FUERA_DE_SERVICIO',
  'marcar-pendiente-preparacion': 'PENDIENTE_PREPARACION',
}

/** API simulada que recuerda el estado de cada apartamento. */
function apiCon({ apartamentos = APARTAMENTOS, rechazar = null }: {
  apartamentos?: ApartamentoOperativo[]
  rechazar?: [number, unknown] | null
} = {}) {
  const estado = { lista: apartamentos.map((a) => ({ ...a })) }
  const pendientes = () => estado.lista.filter((a) => a.estadoOperativo === 'PENDIENTE_PREPARACION' || a.estadoOperativo === 'EN_PREPARACION')
  const pasos = Object.fromEntries(estado.lista.flatMap((apartamento) => Object.keys(DESTINO).map((paso) => [
    `/api/apartamentos/${apartamento.codigo}/${paso}`,
    (): [number, unknown] => {
      if (rechazar) return rechazar
      const cambiado = estado.lista.find((a) => a.codigo === apartamento.codigo)!
      cambiado.estadoOperativo = DESTINO[paso]
      return [200, cambiado]
    },
  ])))
  return laApiRespondePorRuta({
    '/api/apartamentos/pendientes-preparacion': () => [200, paginaDe(pendientes())],
    '/api/apartamentos': () => [200, paginaDe(estado.lista)],
    '/api/alojamiento': [200, { nombre: 'Puerta al Sol' }],
    ...pasos,
  })
}

function abrir(rol: Rol = 'PERSONAL_SERVICIO', direccion = '/preparacion') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

describe('preparación de apartamentos (CU-41 · CU-42)', () => {
  it('el personal de servicio ve primero lo ya empezado y una sola acción por apartamento', async () => {
    apiCon()
    abrir()

    expect(await screen.findByRole('heading', { level: 1, name: 'Por preparar' })).toBeInTheDocument()
    expect(screen.getByText('2 apartamentos')).toBeInTheDocument()
    const empezados = screen.getByRole('region', { name: 'Ya empezados (1)' })
    expect(within(empezados).getByRole('button', { name: 'Marcar como preparado: APT-103' })).toBeInTheDocument()
    const pendientes = screen.getByRole('region', { name: 'Pendientes (1)' })
    expect(within(pendientes).getByRole('button', { name: 'Empezar a preparar: APT-101' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Declarar fuera de servicio/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
  })

  it('empezar a preparar lo pasa a «Ya empezados»', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Empezar a preparar: APT-101' }))

    expect(await screen.findByRole('region', { name: 'Ya empezados (2)' })).toBeInTheDocument()
    expect(fetch.mock.calls.some(([url, opciones]) =>
      url === '/api/apartamentos/APT-101/iniciar-preparacion' && opciones?.method === 'PUT')).toBe(true)
  })

  it('marcar como preparado pide confirmación y, al hacerlo, sale de la lista con un aviso', async () => {
    apiCon()
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Marcar como preparado: APT-103' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Marcar como preparado' })
    expect(within(dialogo).getByText(/quedó listo para entregar/)).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Sí, está preparado' }))

    expect(await screen.findByText('APT-103 quedó preparado. Recepción ya puede entregarlo.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como preparado: APT-103' })).not.toBeInTheDocument()
  })

  it('si otra persona ya lo cambió, lo dice y la lista se actualiza', async () => {
    apiCon({ rechazar: [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Transición no permitida', detalles: [] }] })
    const usuario = abrir()

    await usuario.click(await screen.findByRole('button', { name: 'Empezar a preparar: APT-101' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Otra persona ya lo cambió')
  })

  it('sin nada por preparar lo dice y deja actualizar', async () => {
    apiCon({ apartamentos: [APARTAMENTOS[1]] })
    abrir()

    expect(await screen.findByText('Todo está preparado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Actualizar' })).toBeInTheDocument()
  })

  it('el administrador ve todos: fuera de servicio con confirmación, volver a preparación y el ocupado sin acciones', async () => {
    const fetch = apiCon()
    const usuario = abrir('ADMINISTRADOR')

    await usuario.click(await screen.findByRole('tab', { name: /Todos/ }))
    const panel = screen.getByRole('tabpanel')
    expect(within(panel).getByText('Se libera al registrar la salida.')).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Declarar fuera de servicio: APT-104' })).not.toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: 'Volver a preparación: APT-110' })).toBeInTheDocument()

    await usuario.click(within(panel).getByRole('button', { name: 'Declarar fuera de servicio: APT-102' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Declarar fuera de servicio' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Declarar fuera de servicio' }))

    expect(await screen.findByText(/APT-102 quedó fuera de servicio/)).toBeInTheDocument()
    expect(fetch.mock.calls.some(([url]) => url === '/api/apartamentos/APT-102/declarar-fuera-de-servicio')).toBe(true)
  })

  it('recepción no puede abrirla', () => {
    apiCon()
    abrir('RECEPCIONISTA')

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('la bienvenida del personal de servicio dice cuánto hay por preparar', async () => {
    apiCon()
    abrir('PERSONAL_SERVICIO', '/')

    const acceso = await screen.findByRole('link', { name: /Por preparar/ })
    expect(acceso).toHaveAttribute('href', '/preparacion')
    expect(await within(acceso).findByText('2 apartamentos por preparar.')).toBeInTheDocument()
  })
})
