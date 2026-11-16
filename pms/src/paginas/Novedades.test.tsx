import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Novedad } from '../api/novedades'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const paso = (estado: Novedad['estado'], autorNombre: string | null, fechaHora: string) =>
  ({ estado, autor: 'USR-4', autorNombre, fechaHora })

const VIDRIO: Novedad = {
  id: 'NOV-3', apartamento: 'APT-104', descripcion: 'Vidrio de la ventana del cuarto roto', gravedad: 'ALTA',
  estado: 'ABIERTA', historial: [paso('ABIERTA', 'Luz Marina Cardona', '2026-11-17T09:15:00')],
}
const GRIFO: Novedad = {
  id: 'NOV-1', apartamento: 'APT-101', descripcion: 'El grifo del lavamanos gotea', gravedad: 'MEDIA',
  estado: 'EN_REVISION', historial: [
    paso('ABIERTA', 'Luz Marina Cardona', '2026-11-15T16:02:00'),
    paso('EN_REVISION', null, '2026-11-16T08:30:00'),
  ],
}

function abrir(ruta = '/novedades', rol: Rol = 'PERSONAL_SERVICIO') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

const consultas = (fetch: ReturnType<typeof laApiRespondePorRuta>) => fetch.mock.calls
  .map(([url, o]) => ({ url: String(url), metodo: (o as RequestInit | undefined)?.method ?? 'GET', cuerpo: (o as RequestInit | undefined)?.body }))
const tabla = () => screen.findByRole('table', { name: 'Novedades' })

describe('novedades', () => {
  it('al entrar muestra las por atender, con su gravedad, su estado, quién la reportó y la acción siguiente', async () => {
    const fetch = laApiRespondePorRuta({
      '/api/novedades': [200, paginaDe([VIDRIO, GRIFO])],
      '/api/apartamentos': [200, paginaDe([{ codigo: 'APT-101' }, { codigo: 'APT-104' }])],
    })
    abrir()
    const filas = within(await tabla()).getAllByRole('row').slice(1)
    expect(filas[0]).toHaveTextContent('APT-104')
    expect(filas[0]).toHaveTextContent('Gravedad: Alta')
    expect(filas[0]).toHaveTextContent(/Estado: ○ Abierta/)
    expect(filas[0]).toHaveTextContent('Reportada')
    expect(filas[0]).toHaveTextContent('por Luz Marina Cardona')
    expect(within(filas[0]).getByRole('button', { name: 'Pasar a revisión: novedad de APT-104' })).toBeInTheDocument()
    expect(within(filas[1]).getByRole('button', { name: 'Cerrar: novedad de APT-101' })).toBeInTheDocument()
    expect(consultas(fetch).find((c) => c.url.startsWith('/api/novedades'))?.url)
      .toBe('/api/novedades?estado=ABIERTA%2CEN_REVISION&pagina=0')
    expect(screen.getByRole('button', { name: /^Filtros \(1\)/ })).toHaveTextContent('Por atender')
  })

  it('sin nada por atender lo dice, y con filtros que no coinciden ofrece limpiarlos', async () => {
    const fetch = laApiRespondePorRuta({ '/api/novedades': [200, paginaDe([])] })
    const persona = abrir()
    expect(await screen.findByRole('heading', { name: 'No hay novedades por atender' })).toBeInTheDocument()

    await persona.click(screen.getByRole('checkbox', { name: 'Cerrada' }))
    await persona.click(screen.getByRole('checkbox', { name: 'Alta' }))
    await persona.click(screen.getByRole('button', { name: 'Filtrar' }))
    expect(await screen.findByRole('heading', { name: 'Ninguna novedad coincide con los filtros' })).toBeInTheDocument()
    expect(consultas(fetch).at(-1)?.url).toBe('/api/novedades?estado=ABIERTA%2CEN_REVISION%2CCERRADA&gravedad=ALTA&pagina=0')

    await persona.click(screen.getByRole('button', { name: 'Limpiar filtros' }))
    await waitFor(() => expect(consultas(fetch).at(-1)?.url).toBe('/api/novedades?pagina=0'))
  })

  it('recepción no la puede abrir', async () => {
    laApiRespondePorRuta({})
    abrir('/novedades', 'RECEPCIONISTA')
    expect(await screen.findByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('reporta una novedad: pide los tres datos, la envía y la lista se recarga con el aviso', async () => {
    let reportadas = 0
    const fetch = laApiRespondePorRuta({
      '/api/novedades': () => [200, paginaDe(reportadas > 0 ? [VIDRIO] : [])],
      '/api/apartamentos': [200, paginaDe([{ codigo: 'APT-101' }, { codigo: 'APT-104' }])],
      '/api/apartamentos/APT-104/novedades': () => {
        reportadas++
        return [201, VIDRIO]
      },
    })
    const persona = abrir()
    await persona.click(await screen.findByRole('button', { name: /Reportar novedad/ }))
    const dialogo = screen.getByRole('dialog', { name: 'Reportar novedad' })
    expect(dialogo).toHaveTextContent('Reportarla no bloquea el apartamento')
    await within(dialogo).findByRole('option', { name: 'APT-104' })
    await persona.click(within(dialogo).getByRole('button', { name: 'Reportar' }))
    expect(within(dialogo).getByText('Elige el apartamento.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Describe lo que encontraste.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Elige la gravedad.')).toBeInTheDocument()

    await persona.selectOptions(within(dialogo).getByLabelText('Apartamento'), 'APT-104')
    await persona.type(within(dialogo).getByLabelText('¿Qué encontraste?'), ' Vidrio de la ventana del cuarto roto ')
    await persona.click(within(dialogo).getByRole('radio', { name: /Alta/ }))
    await persona.click(within(dialogo).getByRole('button', { name: 'Reportar' }))

    expect(await screen.findByText('Novedad reportada')).toBeInTheDocument()
    const envio = consultas(fetch).find((c) => c.metodo === 'POST')
    expect(envio?.url).toBe('/api/apartamentos/APT-104/novedades')
    expect(JSON.parse(String(envio?.cuerpo))).toEqual({ descripcion: 'Vidrio de la ventana del cuarto roto', gravedad: 'ALTA' })
    expect(await tabla()).toHaveTextContent('APT-104')
  })

  it('avanza con confirmación; si otra persona ya la movió, avisa y recarga', async () => {
    let intentos = 0
    laApiRespondePorRuta({
      '/api/novedades': [200, paginaDe([GRIFO])],
      '/api/novedades/NOV-1/avanzar': () => (intentos++ === 0
        ? [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Una novedad cerrada no avanza más', detalles: [] }]
        : [200, { ...GRIFO, estado: 'CERRADA', historial: [...GRIFO.historial, paso('CERRADA', 'Ana Gómez', '2026-11-17T10:45:00')] }]),
    })
    const persona = abrir()
    await persona.click(within(await tabla()).getByRole('button', { name: 'Cerrar: novedad de APT-101' }))
    const dialogo = screen.getByRole('dialog', { name: '¿Cerrar la novedad de APT-101?' })
    expect(dialogo).toHaveTextContent('Una novedad cerrada no se puede reabrir.')
    await persona.click(within(dialogo).getByRole('button', { name: 'Cerrar novedad' }))
    expect(await screen.findByText(/Esta novedad ya avanzó/)).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await persona.click(within(await tabla()).getByRole('button', { name: 'Cerrar: novedad de APT-101' }))
    await persona.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cerrar novedad' }))
    expect(await screen.findByText('Novedad cerrada')).toBeInTheDocument()
  })

  it('el historial muestra cada paso con quién y cuándo, y desde ahí también se avanza', async () => {
    laApiRespondePorRuta({ '/api/novedades': [200, paginaDe([GRIFO])] })
    const persona = abrir()
    await persona.click(within(await tabla()).getByRole('button', { name: 'APT-101: ver el historial de la novedad' }))
    const historial = screen.getByRole('dialog', { name: 'Novedad de APT-101' })
    const pasos = within(historial).getAllByRole('listitem')
    expect(pasos[0]).toHaveTextContent('Abierta')
    expect(pasos[0]).toHaveTextContent('Luz Marina Cardona')
    expect(pasos[1]).toHaveTextContent('En revisión')
    expect(pasos[1]).toHaveTextContent('Una persona que ya no está en el sistema')
    expect(pasos[1]).toHaveTextContent('16 nov 2026, 8:30 a. m.')
    await persona.click(within(historial).getByRole('button', { name: 'Cerrar novedad' }))
    expect(screen.getByRole('dialog', { name: '¿Cerrar la novedad de APT-101?' })).toBeInTheDocument()
  })
})
