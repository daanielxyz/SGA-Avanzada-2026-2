import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent, { type UserEvent } from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { paginaDe, reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const ALOJAMIENTO = {
  nombre: 'Puerta al Sol',
  parametros: {
    anticipoPct: 30, horaLimiteNoShow: '22:00:00', umbralEdadFacturable: 12,
    aceptaMascotas: true, limiteMascotas: 2, cargoMascota: 30000, cupoParqueadero: 8,
    cobroParqueadero: 'POR_NOCHE', cargoCarro: 10000, cargoMoto: 5000,
  },
  mediosPago: ['EFECTIVO'],
}
const APARTAMENTO = {
  codigo: 'APT-101', nombre: 'Penthouse Mirador', capacidad: 10, dormitorios: 4,
  imagenPrincipal: null, caracteristicas: ['Terraza'], valorEstimado: 500000,
}
const OTRO = { ...APARTAMENTO, codigo: 'APT-102', nombre: 'Casa Jardín', imagenPrincipal: 'https://fotos.test/a.jpg' }
const COTIZACION = {
  total: 400000,
  desglose: [
    { noche: '2099-03-10', temporada: 'Alta', tarifa: 200000, ocupantesFacturables: 1, subtotal: 200000 },
    { noche: '2099-03-11', temporada: 'Alta', tarifa: 200000, ocupantesFacturables: 1, subtotal: 200000 },
  ],
}

function apiDePrueba(cambios: Parameters<typeof laApiRespondePorRuta>[0] = {}) {
  return laApiRespondePorRuta({
    '/api/alojamiento': [200, ALOJAMIENTO],
    '/api/reservas': [201, reservaDePrueba({ codigo: 'RES-2026-00099', estado: 'PENDIENTE', avisoCuenta: null })],
    '/api/reservas/RES-2026-00099': [200, reservaDePrueba({ codigo: 'RES-2026-00099', estado: 'PENDIENTE' })],
    '/api/apartamentos/disponibles': [200, paginaDe([APARTAMENTO, OTRO])],
    '/api/cotizaciones': [200, COTIZACION],
    '/api/apartamentos/APT-101': [200, { codigo: 'APT-101', nombre: 'Penthouse Mirador', limiteMascotas: null }],
    ...cambios,
  })
}

function abrir(direccion = '/reservas/nueva') {
  entrarComo('RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

async function escribirCantidad(usuario: UserEvent, etiqueta: string, valor: string) {
  const campo = screen.getByLabelText(etiqueta)
  await usuario.clear(campo)
  await usuario.type(campo, valor)
}

function escribirFecha(etiqueta: string | RegExp, valor: string, dentro: HTMLElement = document.body) {
  fireEvent.change(within(dentro).getByLabelText(etiqueta), { target: { value: valor } })
}

async function completarEstancia(usuario: UserEvent, ocupantes = 1) {
  escribirFecha('Entrada', '2099-03-10')
  escribirFecha('Salida', '2099-03-12')
  const campo = screen.getByLabelText('Número de ocupantes')
  await usuario.clear(campo)
  await usuario.type(campo, String(ocupantes))
  await usuario.click(screen.getByRole('button', { name: /Siguiente: apartamento/ }))
}

async function completarTitular(usuario: UserEvent) {
  const titular = screen.getByRole('region', { name: 'Titular' })
  await usuario.type(within(titular).getByLabelText('Nombre completo'), 'Ana Ríos')
  await usuario.type(within(titular).getByLabelText('Número de documento'), '52481')
  await usuario.type(within(titular).getByLabelText('Correo electrónico'), 'ana@correo.test')
  await usuario.type(within(titular).getByLabelText('Teléfono'), '3105550142')
}

async function llegarAlPaso3(usuario: UserEvent, ocupantes = 1) {
  await completarEstancia(usuario, ocupantes)
  await usuario.click(await screen.findByRole('button', { name: 'Elegir Penthouse Mirador' }))
  await screen.findByRole('region', { name: 'Titular' })
}

async function llegarAlPaso4(usuario: UserEvent) {
  await llegarAlPaso3(usuario)
  await completarTitular(usuario)
  escribirFecha('Fecha de nacimiento', '1990-03-02')
  await usuario.click(screen.getByRole('button', { name: /Siguiente: resumen/ }))
  await screen.findByRole('heading', { name: 'Resumen' })
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('llegar a crear una reserva', () => {
  it('el listado lleva a «Nueva reserva»', async () => {
    apiDePrueba({ '/api/reservas': [200, paginaDe([])] })
    abrir('/reservas')

    expect(await screen.findByRole('link', { name: /Nueva reserva/ })).toHaveAttribute('href', '/reservas/nueva')
  })

  it('el personal de servicio no puede abrirla', () => {
    apiDePrueba()
    entrarComo('PERSONAL_SERVICIO')
    render(
      <MemoryRouter initialEntries={['/reservas/nueva']}>
        <ProveedorSesion>
          <App />
        </ProveedorSesion>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })

  it('si se recarga en un paso avanzado, vuelve al primero y lo explica', async () => {
    apiDePrueba()
    abrir('/reservas/nueva?paso=3')

    expect(await screen.findByText('Lo escrito no se conserva al recargar la página.')).toBeInTheDocument()
    expect(screen.getByLabelText('Entrada')).toBeInTheDocument()
  })
})

describe('paso 1: estancia (EST-01)', () => {
  it('calcula las noches sin contar la de salida', async () => {
    apiDePrueba()
    abrir()

    escribirFecha('Entrada', '2099-03-10')
    escribirFecha('Salida', '2099-03-12')

    expect(await screen.findByText('2 noches')).toBeInTheDocument()
  })

  it('una salida que no es posterior se marca y no avanza', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()

    escribirFecha('Entrada', '2099-03-10')
    escribirFecha('Salida', '2099-03-10')
    await usuario.click(screen.getByRole('button', { name: /Siguiente: apartamento/ }))

    expect(screen.getByText('La salida debe ser posterior a la entrada.')).toBeInTheDocument()
    expect(screen.getByLabelText('Salida')).toBeInvalid()
    expect(fetch.mock.calls.some(([url]) => String(url).includes('disponibles'))).toBe(false)
  })

  it('una entrada pasada se marca', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()

    escribirFecha('Entrada', '2001-01-01')
    escribirFecha('Salida', '2099-03-10')
    await usuario.click(screen.getByRole('button', { name: /Siguiente: apartamento/ }))

    expect(screen.getByText(/La entrada no puede ser anterior a hoy/)).toBeInTheDocument()
  })
})

describe('paso 2: apartamento', () => {
  it('muestra las tarjetas con su valor y una foto que falta', async () => {
    apiDePrueba()
    abrir()
    await completarEstancia(userEvent.setup())

    const tarjeta = (await screen.findByRole('heading', { name: 'Penthouse Mirador' })).closest('article') as HTMLElement
    expect(within(tarjeta).getByText('Sin foto')).toBeInTheDocument()
    expect(within(tarjeta).getByText('$ 500.000')).toBeInTheDocument()
    expect(within(tarjeta).getByText('Hasta 10 personas · 4 dormitorios')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 1 · 2 apartamentos')).toBeInTheDocument()
  })

  it('elegir uno avanza de una vez', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()

    await llegarAlPaso3(usuario)

    expect(screen.getByRole('region', { name: 'Titular' })).toBeInTheDocument()
  })

  it('sin disponibles deja cambiar las fechas sin perderlas', async () => {
    apiDePrueba({ '/api/apartamentos/disponibles': [200, paginaDe([])] })
    abrir()
    const usuario = userEvent.setup()
    await completarEstancia(usuario)

    await usuario.click(await screen.findByRole('button', { name: 'Cambiar fechas' }))

    expect(screen.getByLabelText('Entrada')).toHaveValue('2099-03-10')
  })

  it('si falla se puede reintentar', async () => {
    let falla = true
    apiDePrueba({
      '/api/apartamentos/disponibles': () => (falla
        ? [500, { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'Algo falló', detalles: [] }]
        : [200, paginaDe([APARTAMENTO])]),
    })
    abrir()
    const usuario = userEvent.setup()
    await completarEstancia(usuario)

    const error = await screen.findByRole('alert')
    expect(error).toHaveTextContent('No pudimos cargar los apartamentos')
    falla = false
    await usuario.click(within(error).getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('heading', { name: 'Penthouse Mirador' })).toBeInTheDocument()
  })
})

describe('paso 3: personas (TIT-01 · OCU-02 · OCU-05)', () => {
  it('dibuja un ocupante por persona y el titular llena el primero', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario, 3)

    expect(screen.getAllByRole('group', { name: /^Ocupante \d/ })).toHaveLength(3)
    await completarTitular(usuario)

    const primero = screen.getByRole('group', { name: 'Ocupante 1 · titular' })
    expect(within(primero).getByLabelText('Nombre completo')).toHaveValue('Ana Ríos')
    expect(within(primero).getByLabelText(/Número/)).toHaveValue('52481')
  })

  it('marca lo que falta junto a cada campo y no avanza', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario)

    await usuario.click(screen.getByRole('button', { name: /Siguiente: resumen/ }))

    expect(screen.getByText(/Revisa \d+ campos marcados/)).toBeInTheDocument()
    expect(screen.getByText('Escribe el correo.')).toBeInTheDocument()
    expect(screen.getByText('Escribe la fecha de nacimiento.')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Resumen' })).not.toBeInTheDocument()
  })

  it('corregir un campo le quita su error', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario)
    await usuario.click(screen.getByRole('button', { name: /Siguiente: resumen/ }))
    expect(screen.getByText('Escribe la fecha de nacimiento.')).toBeInTheDocument()

    escribirFecha('Fecha de nacimiento', '1990-03-02')

    expect(screen.queryByText('Escribe la fecha de nacimiento.')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Fecha de nacimiento')).toBeValid()
  })

  it('un ocupante repetido se marca (OCU-05)', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario, 2)
    await completarTitular(usuario)

    const segundo = screen.getByRole('group', { name: 'Ocupante 2' })
    await usuario.selectOptions(within(segundo).getByLabelText(/Documento/), 'CC')
    await usuario.type(within(segundo).getByLabelText(/Número/), '52481')
    await usuario.click(screen.getByRole('button', { name: /Siguiente: resumen/ }))

    expect(within(segundo).getByText('Ya está en la lista (ocupante 1).')).toBeInTheDocument()
  })

  it('un titular que no está entre los ocupantes se marca (TIT-01)', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario)
    await completarTitular(usuario)
    await usuario.click(screen.getByRole('checkbox', { name: 'El titular también se aloja' }))
    const primero = screen.getByRole('group', { name: 'Ocupante 1' })
    await usuario.clear(within(primero).getByLabelText(/Número/))
    await usuario.selectOptions(within(primero).getByLabelText(/Documento/), '')

    await usuario.click(screen.getByRole('button', { name: /Siguiente: resumen/ }))

    expect(screen.getByText('El titular debe estar entre los ocupantes, con el mismo documento.')).toBeInTheDocument()
  })
})

describe('paso 4: resumen', () => {
  it('muestra el desglose de la cotización y envía la reserva directa', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)

    expect(await screen.findByRole('table', { name: 'Desglose por noche' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByText('Reserva creada · RES-2026-00099')).toBeInTheDocument()
    expect(screen.getByText('Queda pendiente. Para confirmarla necesitas la hora de llegada y cubrir el anticipo.')).toBeInTheDocument()
    const llamada = fetch.mock.calls.find(([url, opciones]) => url === '/api/reservas' && opciones?.method === 'POST')
    expect(JSON.parse(String(llamada?.[1]?.body))).toMatchObject({
      apartamento: 'APT-101',
      canalOrigen: 'DIRECTO',
      entrada: '2099-03-10',
      salida: '2099-03-12',
      titular: { nombre: 'Ana Ríos', numeroDocumento: '52481' },
      ocupantes: [{ nombre: 'Ana Ríos', fechaNacimiento: '1990-03-02', tipoDocumento: 'CC', numeroDocumento: '52481' }],
      carros: 0,
      motos: 0,
      mascotas: 0,
    })
    expect(JSON.parse(String(llamada?.[1]?.body))).not.toHaveProperty('horaEstimadaLlegada')
    expect(JSON.parse(String(llamada?.[1]?.body))).not.toHaveProperty('vehiculos')
  })

  it('muestra el aviso de cuenta si el titular ya tenía otra', async () => {
    apiDePrueba({
      '/api/reservas': [201, reservaDePrueba({ codigo: 'RES-2026-00099', avisoCuenta: 'Ya tenía una cuenta con otro correo.' })],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByText('Ya tenía una cuenta con otro correo.')).toBeInTheDocument()
  })

  it('suma el parqueadero y las mascotas al total y los envía (RP-02 · RP-03 · DEC-89)', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await escribirCantidad(usuario, 'Carros', '1')
    await escribirCantidad(usuario, 'Motos', '1')
    await escribirCantidad(usuario, 'Número de mascotas', '2')

    // Dos noches: (10.000 + 5.000) × 2 de parqueadero y 2 × 30.000 por las mascotas, sobre 400.000 de estancia
    expect(screen.getByText('$ 490.000')).toBeInTheDocument()
    expect(screen.getByText(/El anticipo incluye el parqueadero y las mascotas\./)).toHaveTextContent(
      'Estancia $ 400.000 + parqueadero $ 30.000 + mascotas $ 60.000',
    )
    // El total cambia mientras se escribe: el lector de pantalla lo anuncia
    expect(screen.getByText('$ 490.000').closest('[aria-live="polite"]')).not.toBeNull()
    expect(screen.queryByText(/van aparte/)).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    await screen.findByText('Reserva creada · RES-2026-00099')
    const llamada = fetch.mock.calls.find(([url, opciones]) => url === '/api/reservas' && opciones?.method === 'POST')
    expect(JSON.parse(String(llamada?.[1]?.body))).toMatchObject({ carros: 1, motos: 1, mascotas: 2 })
  })

  it('el parqueadero por estancia se cobra una sola vez (DEC-89)', async () => {
    apiDePrueba({
      '/api/alojamiento': [200, { ...ALOJAMIENTO, parametros: { ...ALOJAMIENTO.parametros, cobroParqueadero: 'POR_ESTADIA' } }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await escribirCantidad(usuario, 'Carros', '1')

    expect(screen.getByLabelText('Carros')).toHaveAccessibleDescription('$ 10.000 por carro por estancia.')
    expect(screen.getByText('$ 410.000')).toBeInTheDocument()
  })

  it('más carros y motos que cupos se marca y no se envía (RP-02)', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await escribirCantidad(usuario, 'Carros', '5')
    await escribirCantidad(usuario, 'Motos', '4')

    // Un solo error para los dos campos, en el grupo, y los dos marcados
    expect(screen.getByRole('group', { name: 'Parqueadero' })).toHaveAccessibleDescription(
      'El parqueadero tiene 8 cupos en total, entre carros y motos.',
    )
    expect(screen.getByLabelText('Carros')).toBeInvalid()
    expect(screen.getByLabelText('Motos')).toBeInvalid()

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    expect(screen.getByLabelText('Carros')).toHaveFocus()
    expect(fetch.mock.calls.some(([url, o]) => url === '/api/reservas' && o?.method === 'POST')).toBe(false)
  })

  it('usa el límite de mascotas del apartamento, no el del hotel (DEC-93)', async () => {
    const fetch = apiDePrueba({
      '/api/apartamentos/APT-101': [200, { codigo: 'APT-101', nombre: 'Penthouse Mirador', limiteMascotas: 1 }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    expect(await screen.findByText(/Este apartamento acepta hasta 1 mascota,/)).toBeInTheDocument()
    await escribirCantidad(usuario, 'Número de mascotas', '2')

    // Se marca mientras se escribe, sin esperar a enviar
    expect(screen.getByLabelText('Número de mascotas')).toBeInvalid()
    expect(screen.getByLabelText('Número de mascotas')).toHaveAccessibleDescription('Este apartamento acepta hasta 1 mascota.')
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    expect(screen.getByLabelText('Número de mascotas')).toHaveFocus()
    expect(fetch.mock.calls.some(([url, o]) => url === '/api/reservas' && o?.method === 'POST')).toBe(false)
  })

  it('un apartamento con límite 0 no recibe mascotas aunque el hotel sí (DEC-93)', async () => {
    apiDePrueba({
      '/api/apartamentos/APT-101': [200, { codigo: 'APT-101', nombre: 'Penthouse Mirador', limiteMascotas: 0 }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)

    expect(await screen.findByText('Este apartamento no recibe mascotas.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Número de mascotas')).not.toBeInTheDocument()
  })

  it('un apartamento sin límite propio usa el del hotel (DEC-93)', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })
    await waitFor(() => expect(fetch.mock.calls.some(([url]) => url === '/api/apartamentos/APT-101')).toBe(true))

    expect(screen.getByLabelText('Número de mascotas')).toHaveAccessibleDescription(/acepta hasta 2 mascotas,/)
    await escribirCantidad(usuario, 'Número de mascotas', '2')
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    await screen.findByText('Reserva creada · RES-2026-00099')
    const llamada = fetch.mock.calls.find(([url, opciones]) => url === '/api/reservas' && opciones?.method === 'POST')
    expect(JSON.parse(String(llamada?.[1]?.body))).toMatchObject({ mascotas: 2 })
  })

  it('las mascotas escritas para otro apartamento se avisan y no se envían si este no las recibe (DEC-93)', async () => {
    let vendida = true
    const fetch = apiDePrueba({
      '/api/reservas': () => (vendida
        ? [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'El apartamento APT-101 ya está reservado en esas fechas', detalles: [] }]
        : [201, reservaDePrueba({ codigo: 'RES-2026-00099', estado: 'PENDIENTE', avisoCuenta: null })]),
      '/api/apartamentos/APT-102': [200, { codigo: 'APT-102', nombre: 'Casa Jardín', limiteMascotas: 0 }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })
    await escribirCantidad(usuario, 'Número de mascotas', '2')
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    await usuario.click(await screen.findByRole('button', { name: 'Elegir otro apartamento' }))

    vendida = false
    await usuario.click(await screen.findByRole('button', { name: 'Elegir Casa Jardín' }))
    await screen.findByRole('table', { name: 'Desglose por noche' })

    expect(await screen.findByText(/Este apartamento no recibe mascotas: la reserva se creará sin las 2 que escribiste\./))
      .toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    await screen.findByText('Reserva creada · RES-2026-00099')
    const creada = fetch.mock.calls.filter(([url, o]) => url === '/api/reservas' && o?.method === 'POST').at(-1)
    expect(JSON.parse(String(creada?.[1]?.body))).toMatchObject({ apartamento: 'APT-102', mascotas: 0 })
  })

  it('un rechazo con motivo ofrece solo las salidas que lo resuelven (DEC-94)', async () => {
    apiDePrueba({
      '/api/reservas': [409, {
        estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La estancia debe ser de al menos 3 noches en esta temporada',
        detalles: [], motivo: 'ESTANCIA_MINIMA',
      }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByRole('button', { name: 'Cambiar fechas' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Elegir otro apartamento' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Revisar personas' })).not.toBeInTheDocument()
  })

  it('si no se puede consultar el apartamento, rige el límite del hotel (DEC-93)', async () => {
    apiDePrueba({ '/api/apartamentos/APT-101': [500, { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'Falló', detalles: [] }] })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    expect(screen.getByLabelText('Número de mascotas')).toHaveAccessibleDescription(/acepta hasta 2 mascotas,/)
  })

  it('más mascotas que el límite se marca y no se envía (RP-03)', async () => {
    const fetch = apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    const mascotas = screen.getByLabelText('Número de mascotas')
    await usuario.clear(mascotas)
    await usuario.type(mascotas, '3')
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(mascotas).toBeInvalid()
    expect(fetch.mock.calls.some(([url, o]) => url === '/api/reservas' && o?.method === 'POST')).toBe(false)
  })

  it('un hotel que no acepta mascotas no muestra el campo', async () => {
    apiDePrueba({
      '/api/alojamiento': [200, { ...ALOJAMIENTO, parametros: { ...ALOJAMIENTO.parametros, aceptaMascotas: false } }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)

    expect(screen.queryByLabelText('Número de mascotas')).not.toBeInTheDocument()
  })

  it('un hotel sin cupo de parqueadero no pide vehículos (RP-02)', async () => {
    apiDePrueba({
      '/api/alojamiento': [200, { ...ALOJAMIENTO, parametros: { ...ALOJAMIENTO.parametros, cupoParqueadero: 0 } }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)

    expect(screen.queryByLabelText('Carros')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Motos')).not.toBeInTheDocument()
  })

  it('con cupo de parqueadero pide carros y motos, con su precio y los cupos (RP-02 · DEC-89)', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)

    expect(screen.getByLabelText('Carros')).toHaveAccessibleDescription('$ 10.000 por carro cada noche.')
    expect(screen.getByLabelText('Motos')).toHaveAccessibleDescription('$ 5.000 por moto cada noche.')
    expect(screen.getByRole('group', { name: 'Parqueadero' })).toHaveAccessibleDescription(
      'Carros y motos comparten 8 cupos con las demás reservas.',
    )
  })

  it('una noche ya vendida no pierde datos: se elige otro apartamento y vuelve al resumen (RN-01)', async () => {
    let vendida = true
    const creada = reservaDePrueba({ codigo: 'RES-2026-00099', estado: 'PENDIENTE', avisoCuenta: null })
    apiDePrueba({
      '/api/reservas': () => (vendida
        ? [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'El apartamento APT-101 ya está reservado en esas fechas (RES-2026-00001)', detalles: [] }]
        : [201, creada]),
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    const error = await screen.findByRole('alert')
    expect(error).toHaveTextContent('ya está reservado en esas fechas')
    expect(error).toHaveTextContent('No se perdió ningún dato.')

    await usuario.click(screen.getByRole('button', { name: 'Elegir otro apartamento' }))
    expect(await screen.findByText(/Conservamos las fechas, las personas/)).toBeInTheDocument()

    vendida = false
    await usuario.click(await screen.findByRole('button', { name: 'Elegir Casa Jardín' }))
    await screen.findByRole('heading', { name: 'Resumen' })
    expect(screen.getByText('Ana Ríos', { exact: false })).toBeInTheDocument()

    await screen.findByRole('table', { name: 'Desglose por noche' })
    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))
    expect(await screen.findByText('Reserva creada · RES-2026-00099')).toBeInTheDocument()
  })

  it('un 400 muestra los errores por campo', async () => {
    apiDePrueba({
      '/api/reservas': [400, { estado: 400, codigo: 'VALIDACION', mensaje: 'La solicitud tiene datos inválidos', detalles: ['titular.correo: no tiene formato de correo'] }],
    })
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso4(usuario)
    await screen.findByRole('table', { name: 'Desglose por noche' })

    await usuario.click(screen.getByRole('button', { name: 'Crear reserva' }))

    expect(await screen.findByText('titular.correo: no tiene formato de correo')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Elegir otro apartamento' })).not.toBeInTheDocument()
  })
})

describe('moverse sin perder lo escrito', () => {
  it('«Atrás» conserva las personas', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario)
    await completarTitular(usuario)

    await usuario.click(screen.getByRole('button', { name: /Atrás/ }))
    await screen.findByRole('heading', { name: 'Apartamentos disponibles' })
    await usuario.click(await screen.findByRole('button', { name: 'Continuar con Penthouse Mirador' }))

    const titular = await screen.findByRole('region', { name: 'Titular' })
    expect(within(titular).getByLabelText('Correo electrónico')).toHaveValue('ana@correo.test')
  })

  it('cambiar el número de ocupantes conserva a los que ya estaban', async () => {
    apiDePrueba()
    abrir()
    const usuario = userEvent.setup()
    await llegarAlPaso3(usuario, 2)
    await completarTitular(usuario)
    await usuario.type(within(screen.getByRole('group', { name: 'Ocupante 2' })).getByLabelText('Nombre completo'), 'Luis Ríos')

    await usuario.click(screen.getByRole('button', { name: /Atrás/ }))
    await usuario.click(await screen.findByRole('button', { name: /Atrás/ }))
    const campo = await screen.findByLabelText('Número de ocupantes')
    await usuario.clear(campo)
    await usuario.type(campo, '3')
    await usuario.click(screen.getByRole('button', { name: /Siguiente: apartamento/ }))
    await usuario.click(await screen.findByRole('button', { name: 'Elegir Penthouse Mirador' }))

    await waitFor(() => expect(screen.getAllByRole('group', { name: /^Ocupante \d/ })).toHaveLength(3))
    expect(within(screen.getByRole('group', { name: 'Ocupante 2' })).getByLabelText('Nombre completo')).toHaveValue('Luis Ríos')
  })

  it('salir por un enlace con algo escrito pregunta antes', async () => {
    apiDePrueba({ '/api/reservas': [200, paginaDe([])] })
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false)
    abrir()
    const usuario = userEvent.setup()

    escribirFecha('Entrada', '2099-03-10')
    await usuario.click(screen.getByRole('link', { name: 'Volver a reservas' }))

    expect(confirmar).toHaveBeenCalled()
    expect(screen.getByRole('heading', { level: 1, name: 'Nueva reserva' })).toBeInTheDocument()
  })

  it('sin nada escrito sale sin preguntar', async () => {
    apiDePrueba({ '/api/reservas': [200, paginaDe([])] })
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false)
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('link', { name: 'Volver a reservas' }))

    expect(confirmar).not.toHaveBeenCalled()
    expect(await screen.findByRole('heading', { level: 1, name: 'Reservas' })).toBeInTheDocument()
  })
})
