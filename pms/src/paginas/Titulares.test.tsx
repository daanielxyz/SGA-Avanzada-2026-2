import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Titular } from '../api/titulares'
import { paginaDe, reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const ANA: Titular = {
  id: 'TIT-1', nombre: 'Ana María Ríos Pérez', nombres: 'Ana María', primerApellido: 'Ríos', segundoApellido: 'Pérez',
  tipoDocumento: 'CC', numeroDocumento: '1094000', correo: 'ana.rios@ejemplo.co', telefono: '300 123 4567',
  validado: true, validadoEn: '2026-11-14T15:24:00', validadoPor: 'USR-2', validadoPorNombre: 'María Fernanda Ospina',
}
const ANDRES: Titular = {
  id: 'TIT-2', nombre: 'Andrés Felipe Gómez', nombres: 'Andrés Felipe', primerApellido: 'Gómez', segundoApellido: null,
  tipoDocumento: 'PASAPORTE', numeroDocumento: 'AB123456', correo: null, telefono: '+1 305 555 0142',
  validado: false, validadoEn: null, validadoPor: null, validadoPorNombre: null,
}

const error = (estado: number, codigo: string, mensaje: string, motivo?: string): [number, unknown] =>
  [estado, { estado, codigo, mensaje, detalles: [], motivo }]

function abrir(ruta = '/titulares', rol: Rol = 'RECEPCIONISTA') {
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

const consultas = (fetch: ReturnType<typeof laApiRespondePorRuta>, ruta: string) => fetch.mock.calls
  .map(([url]) => String(url)).filter((url) => url.split('?')[0] === ruta)
const cuerpo = (fetch: ReturnType<typeof laApiRespondePorRuta>, ruta: string) => {
  const llamada = fetch.mock.calls.filter(([url, o]) => url === ruta && (o as RequestInit | undefined)?.method === 'PUT').at(-1)
  return llamada ? JSON.parse(String((llamada[1] as RequestInit).body ?? 'null')) : undefined
}

describe('titulares', () => {
  it('lista cada titular con su documento legible, su contacto y su identidad', async () => {
    laApiRespondePorRuta({ '/api/titulares': [200, paginaDe([ANA, ANDRES])] })
    abrir()
    const tabla = await screen.findByRole('table', { name: 'Titulares' })
    const ana = within(tabla).getByRole('row', { name: /Ana María Ríos Pérez/ })
    expect(ana).toHaveTextContent('CC 1.094.000')
    expect(ana).toHaveTextContent('ana.rios@ejemplo.co')
    expect(ana).toHaveTextContent('Validada')
    const andres = within(tabla).getByRole('row', { name: /Andrés Felipe Gómez/ })
    expect(andres).toHaveTextContent('Pasaporte AB123456')
    expect(andres).toHaveTextContent('Sin validar')
    expect(andres).toHaveTextContent('Sin correo')
  })

  it('busca con lo escrito y, si nada coincide, ofrece limpiar la búsqueda', async () => {
    const fetch = laApiRespondePorRuta({
      '/api/titulares': () => [200, paginaDe(consultas(fetch, '/api/titulares').at(-1)?.includes('buscar=') ? [] : [ANA])],
    })
    const persona = abrir()
    await screen.findByRole('table', { name: 'Titulares' })
    await persona.type(screen.getByLabelText('Busca por nombre o documento'), ' rodriguez ')
    await persona.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('heading', { name: 'Ningún titular coincide con «rodriguez»' })).toBeInTheDocument()
    expect(consultas(fetch, '/api/titulares').at(-1)).toBe('/api/titulares?pagina=0&buscar=rodriguez')
    await persona.click(screen.getByRole('button', { name: 'Limpiar búsqueda' }))
    expect(await screen.findByRole('table', { name: 'Titulares' })).toBeInTheDocument()
    expect(screen.getByLabelText('Busca por nombre o documento')).toHaveValue('')
  })

  it('el personal de servicio no la puede abrir', async () => {
    laApiRespondePorRuta({})
    abrir('/titulares', 'PERSONAL_SERVICIO')
    expect(await screen.findByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('el detalle muestra quién validó, los datos y sus reservas con el total con IVA, y vuelve a la búsqueda', async () => {
    laApiRespondePorRuta({
      '/api/titulares': [200, paginaDe([ANA])],
      '/api/titulares/TIT-1': [200, ANA],
      '/api/reservas': [200, paginaDe([reservaDePrueba({ codigo: 'RES-2026-00051', estado: 'CONFIRMADA',
        valor: { ...reservaDePrueba().valor, total: 1000000, iva: 190000, totalConIva: 1190000, ivaPorcentaje: 19 } })])],
    })
    const persona = abrir('/titulares?buscar=ana')
    await persona.click(await screen.findByRole('link', { name: 'Ana María Ríos Pérez' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ana María Ríos Pérez' })).toBeInTheDocument()
    expect(screen.getByText(/Identidad validada · 14 nov 2026, 3:24 p\. m\., por María Fernanda Ospina/)).toBeInTheDocument()
    expect(screen.getByText('Segundo apellido').nextSibling).toHaveTextContent('Pérez')
    expect(screen.queryByRole('button', { name: 'Validar identidad' })).not.toBeInTheDocument()
    const reservas = await screen.findByRole('table', { name: 'Reservas del titular' })
    expect(within(reservas).getByRole('link', { name: 'RES-2026-00051' })).toBeInTheDocument()
    expect(reservas).toHaveTextContent('$ 1.190.000')
    expect(screen.getAllByRole('link', { name: 'Titulares' }).map((l) => l.getAttribute('href'))).toContain('/titulares?buscar=ana')
  })

  it('edita el nombre y el contacto: pide nombres y al menos un contacto, y manda lo opcional como nulo', async () => {
    const fetch = laApiRespondePorRuta({
      '/api/titulares/TIT-2': (metodo) => [200, metodo === 'PUT' ? { ...ANDRES, nombre: 'Andrés Felipe Gómez Ruiz', segundoApellido: 'Ruiz' } : ANDRES],
      '/api/reservas': [200, paginaDe([])],
    })
    const persona = abrir('/titulares/TIT-2')
    await persona.click(await screen.findByRole('button', { name: 'Editar datos' }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar datos' })
    expect(dialogo).toHaveTextContent('Pasaporte AB123456')
    expect(within(dialogo).queryByRole('textbox', { name: /Documento/ })).not.toBeInTheDocument()

    await persona.clear(within(dialogo).getByLabelText('Nombres'))
    await persona.clear(within(dialogo).getByLabelText('Teléfono'))
    await persona.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))
    expect(within(dialogo).getByText('Escribe los nombres.')).toBeInTheDocument()
    expect(within(dialogo).getByText('Deja al menos un correo o un teléfono.')).toBeInTheDocument()

    await persona.type(within(dialogo).getByLabelText('Nombres'), 'Andrés Felipe')
    await persona.type(within(dialogo).getByLabelText(/Segundo apellido/), 'Ruiz')
    await persona.type(within(dialogo).getByLabelText('Teléfono'), '+1 305 555 0142')
    await persona.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    expect(await screen.findByText('Datos guardados')).toBeInTheDocument()
    expect(cuerpo(fetch, '/api/titulares/TIT-2')).toEqual({
      nombres: 'Andrés Felipe', primerApellido: 'Gómez', segundoApellido: 'Ruiz', correo: null, telefono: '+1 305 555 0142',
    })
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Andrés Felipe Gómez Ruiz')
  })

  it('valida la identidad una vez; si otra persona ya lo hizo, avisa y recarga', async () => {
    let validaciones = 0
    laApiRespondePorRuta({
      '/api/titulares/TIT-2': [200, ANDRES],
      '/api/titulares/TIT-2/validar': () => (validaciones++ === 0
        ? error(409, 'REGLA_NEGOCIO', 'Ya estaba validada', 'IDENTIDAD_VALIDADA')
        : [200, { ...ANDRES, validado: true, validadoEn: '2026-11-17T10:15:00', validadoPorNombre: 'Ana Gómez' }]),
      '/api/reservas': [200, paginaDe([])],
    })
    const persona = abrir('/titulares/TIT-2')
    await persona.click(await screen.findByRole('button', { name: 'Validar identidad' }))
    const dialogo = screen.getByRole('dialog', { name: '¿Revisaste el documento original de Andrés Felipe Gómez?' })
    expect(dialogo).toHaveTextContent('Pasaporte AB123456')
    await persona.click(within(dialogo).getByRole('button', { name: 'Sí, validar identidad' }))
    expect(await screen.findByText(/La identidad ya estaba validada/)).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    await persona.click(await screen.findByRole('button', { name: 'Validar identidad' }))
    await persona.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sí, validar identidad' }))
    expect(await screen.findByText(/Identidad validada · 17 nov 2026, 10:15 a\. m\., por Ana Gómez/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Validar identidad' })).not.toBeInTheDocument()
  })

  it('un titular que no existe ofrece volver', async () => {
    laApiRespondePorRuta({ '/api/reservas': [200, paginaDe([])] })
    abrir('/titulares/TIT-99')
    expect(await screen.findByRole('heading', { name: 'Este titular no existe' })).toBeInTheDocument()
  })
})
