import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Alojamiento } from '../api/alojamiento'
import { alojamientoDePrueba } from '../pruebas/alojamientoDePrueba'
import { entrarComo, laApiRespondePorRuta, llamada } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RECHAZO = (mensaje: string): [number, unknown] => [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje, detalles: [] }]

/** API simulada que recuerda el alojamiento: cada acción responde el alojamiento ya cambiado. */
function apiCon({ inicial = alojamientoDePrueba(), rechazos = {} }: {
  inicial?: Alojamiento
  rechazos?: Record<string, [number, unknown]>
} = {}) {
  let actual = inicial
  const accion = (ruta: string, cambiar: () => Partial<Alojamiento>) => ({
    [ruta]: (metodo: string): [number, unknown] => {
      if (metodo === 'GET') return [200, actual]
      if (rechazos[ruta]) return rechazos[ruta]
      actual = { ...actual, ...cambiar() }
      return [200, actual]
    },
  })
  const fetch = laApiRespondePorRuta({
    '/api/alojamiento': () => [200, actual],
    ...accion('/api/alojamiento/datos', () => ({ nombre: 'Puerta del Sol' })),
    ...accion('/api/alojamiento/parametros', () => ({})),
    ...accion('/api/alojamiento/servicios', () => ({
      serviciosAdicionales: [...(actual.serviciosAdicionales ?? []), { id: 'SRV-9', nombre: 'Lavandería', generaCargo: true, valor: 18000, activo: true }],
    })),
    ...accion('/api/alojamiento/servicios/SRV-1/desactivar', () => ({
      serviciosAdicionales: actual.serviciosAdicionales?.map((s) => (s.id === 'SRV-1' ? { ...s, activo: false } : s)),
    })),
    ...accion('/api/alojamiento/medios-pago/TARJETA/deshabilitar', () => ({ mediosPago: ['EFECTIVO', 'TRANSFERENCIA'] })),
    ...accion('/api/alojamiento/marca', () => ({})),
  })
  return fetch
}

function abrir(rol: 'ADMINISTRADOR' | 'RECEPCIONISTA' = 'ADMINISTRADOR') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={['/alojamiento']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

function envios(fetch: ReturnType<typeof apiCon>) {
  return fetch.mock.calls.map((_, n) => llamada(fetch, n)).filter((l) => l.metodo !== 'GET')
}

describe('alojamiento (CU-50 · CU-51 · CU-53)', () => {
  it('muestra el nombre como título y las cinco pestañas, con Datos abierta', async () => {
    apiCon()
    abrir()

    expect(await screen.findByRole('heading', { level: 1, name: 'Puerta al Sol' })).toBeInTheDocument()
    expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual([
      'Datos', 'Reglas de la estancia', 'Servicios adicionales', 'Medios de pago', 'Marca',
    ])
    expect(screen.getByRole('tab', { name: 'Datos' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('Nombre')).toHaveValue('Puerta al Sol')
    expect(screen.getByLabelText('Latitud')).toHaveValue('4.6372')
  })

  it('guarda los datos y avisa en el pie del bloque', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    const nombre = await screen.findByLabelText('Nombre')

    await usuario.clear(nombre)
    await usuario.type(nombre, 'Puerta del Sol')
    expect(screen.getByText('Cambios sin guardar')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar datos' }))

    expect(await screen.findByText('Datos guardados')).toBeInTheDocument()
    expect(envios(fetch)).toEqual([expect.objectContaining({
      url: '/api/alojamiento/datos',
      metodo: 'PUT',
      cuerpo: {
        nombre: 'Puerta del Sol', descripcion: 'Apartamentos en la vereda.', ciudad: 'Salento (Quindío)',
        direccion: 'Vereda Palestina km 2', normas: null,
      },
    })])
    expect(await screen.findByRole('heading', { level: 1, name: 'Puerta del Sol' })).toBeInTheDocument()
  })

  it('sin descripción no envía y lo marca junto al campo', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.clear(await screen.findByLabelText('Descripción'))
    await usuario.click(screen.getByRole('button', { name: 'Guardar datos' }))

    expect(screen.getByText('Escribe la descripción.')).toBeInTheDocument()
    expect(screen.getByLabelText('Descripción')).toHaveAttribute('aria-invalid', 'true')
    expect(within(screen.getByRole('alert')).getByText('Error · No se guardaron los datos')).toBeInTheDocument()
    expect(envios(fetch)).toEqual([])
  })

  it('las reglas se envían todas, incluso con mascotas ocultas, y el 409 queda en el bloque', async () => {
    const fetch = apiCon({ rechazos: { '/api/alojamiento/parametros': RECHAZO('El plazo de confirmación debe ser mayor que cero') } })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('tab', { name: 'Reglas de la estancia' }))
    expect(screen.getByText(/Aplica a las reservas nuevas/)).toBeInTheDocument()

    await usuario.click(screen.getByRole('radio', { name: 'No' }))
    expect(screen.queryByLabelText('Máximo por reserva')).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar reglas' }))

    expect(await screen.findByText('El plazo de confirmación debe ser mayor que cero')).toBeInTheDocument()
    const [envio] = envios(fetch)
    expect(envio.url).toBe('/api/alojamiento/parametros')
    expect(Object.keys(envio.cuerpo)).toHaveLength(21)
    expect(envio.cuerpo).toMatchObject({ aceptaMascotas: false, limiteMascotas: 2, cargoMascota: 30000, horaEntrada: '15:00:00' })
  })

  it('pregunta antes de cambiar de pestaña con cambios sin guardar', async () => {
    apiCon()
    const usuario = abrir()
    await usuario.type(await screen.findByLabelText('Ciudad'), ' centro')
    await usuario.click(screen.getByRole('tab', { name: 'Marca' }))

    const dialogo = screen.getByRole('dialog', { name: '¿Salir sin guardar?' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Seguir editando' }))
    expect(screen.getByLabelText('Ciudad')).toHaveValue('Salento (Quindío) centro')

    await usuario.click(screen.getByRole('tab', { name: 'Marca' }))
    await usuario.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Salir sin guardar' }))
    expect(screen.getByRole('tab', { name: 'Marca' })).toHaveAttribute('aria-selected', 'true')
  })

  it('servicios: el desactivado queda sin acciones; crear y desactivar se guardan al confirmar', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('tab', { name: 'Servicios adicionales' }))

    expect(screen.getByRole('heading', { name: 'Servicios adicionales (3)' })).toBeInTheDocument()
    expect(screen.getByText('Desactivado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Desactivar: Transporte al pueblo' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cambiar valor: Wifi' })).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Nuevo servicio' }))
    const nuevo = screen.getByRole('dialog', { name: 'Nuevo servicio' })
    await usuario.click(within(nuevo).getByRole('button', { name: 'Crear servicio' }))
    expect(within(nuevo).getByText('Escribe el nombre del servicio.')).toBeInTheDocument()
    await usuario.type(within(nuevo).getByLabelText('Nombre'), 'Lavandería')
    await usuario.type(within(nuevo).getByLabelText('Valor'), '18.000')
    await usuario.click(within(nuevo).getByRole('button', { name: 'Crear servicio' }))
    expect(await screen.findByText('Lavandería creado')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Desactivar: Desayuno' }))
    const confirmar = screen.getByRole('dialog', { name: '¿Desactivar Desayuno?' })
    expect(within(confirmar).getByText(/No se puede volver a activar/)).toBeInTheDocument()
    await usuario.click(within(confirmar).getByRole('button', { name: 'Sí, desactivar' }))
    expect(await screen.findByText('Desayuno desactivado')).toBeInTheDocument()

    expect(envios(fetch).map((e) => [e.url, e.cuerpo])).toEqual([
      ['/api/alojamiento/servicios', { nombre: 'Lavandería', generaCargo: true, valor: 18000 }],
      ['/api/alojamiento/servicios/SRV-1/desactivar', undefined],
    ])
  })

  it('medios de pago: el rechazo por el mínimo se muestra en el bloque', async () => {
    const fetch = apiCon({
      rechazos: { '/api/alojamiento/medios-pago/TARJETA/deshabilitar': RECHAZO('El alojamiento debe tener al menos 2 medios de pago') },
    })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('tab', { name: 'Medios de pago' }))
    await usuario.click(screen.getByRole('button', { name: 'Deshabilitar: Tarjeta' }))

    expect(await screen.findByText('Error · No se deshabilitó Tarjeta')).toBeInTheDocument()
    expect(screen.getByText('El alojamiento debe tener al menos 2 medios de pago')).toBeInTheDocument()
    expect(envios(fetch)[0]).toMatchObject({ url: '/api/alojamiento/medios-pago/TARJETA/deshabilitar', metodo: 'PUT' })

    await usuario.type(screen.getByLabelText('Agregar medio de pago'), 'efectivo')
    await usuario.click(screen.getByRole('button', { name: 'Agregar' }))
    expect(screen.getByText('Efectivo ya está habilitado.')).toBeInTheDocument()
  })

  it('marca: mide el contraste de cada par y avisa cuál cuesta leer', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('tab', { name: 'Marca' }))

    const fila = screen.getByText('Texto sobre acento, en el acento').closest('li')!
    expect(within(fila).getByText(/difícil de leer/)).toBeInTheDocument()
    expect(within(screen.getByText('Texto sobre fondo').closest('li')!).getByText(/se lee bien/)).toBeInTheDocument()

    const acento = screen.getByRole('textbox', { name: 'Acento' })
    await usuario.clear(acento)
    await usuario.type(acento, '#7A3E0B')
    expect(within(fila).getByText(/se lee bien/)).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Guardar marca' }))
    expect(await screen.findByText('Marca guardada')).toBeInTheDocument()
    expect(envios(fetch)[0].cuerpo).toMatchObject({ colores: { acento: '#7A3E0B' }, radio: { grande: '16px' }, logoUrl: null })
  })

  it('error al cargar: ofrece reintentar', async () => {
    laApiRespondePorRuta({ '/api/alojamiento': [500, { estado: 500, codigo: 'ERROR', mensaje: 'Falla', detalles: [] }] })
    abrir()
    expect(await screen.findByText('No se pudo cargar el alojamiento')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('recepción no ve la pantalla', async () => {
    apiCon()
    abrir('RECEPCIONISTA')
    expect(await screen.findByText('No tienes permiso para ver esta pantalla')).toBeInTheDocument()
  })
})
