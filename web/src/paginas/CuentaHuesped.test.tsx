import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { borrarSesion, guardarSesion, leerSesion } from '../sesion/almacen'

const respuesta = (cuerpo: unknown, estado = 200) => new Response(JSON.stringify(cuerpo), { status: estado })
const errorApi = (estado: number, codigo: string, mensaje: string) =>
  respuesta({ estado, codigo, mensaje, detalles: [] }, estado)

const MANANA = new Date(Date.now() + 86400000).toISOString()
const SESION = { token: 'token-de-prueba', expiraEn: MANANA, nombre: 'Ana Ríos' }

const RESERVA = {
  codigo: 'RES-2026-00042',
  apartamento: 'APT-101',
  entrada: '2027-03-15',
  salida: '2027-03-17',
  estado: 'CONFIRMADA',
  ocupantes: [{ id: 'OCU-1', nombre: 'Ana Ríos' }, { id: 'OCU-2', nombre: 'Luis Ríos' }],
  valor: { desglose: [], total: 360000 },
  extras: { parqueadero: 46000, mascotas: 30000, total: 76000 },
  carros: 1,
  motos: 1,
  mascotas: 1,
}

const SUITE = { codigo: 'APT-101', nombre: 'Suite Jardín', imagenes: [] }
const FOLIO = { totalPagado: 130800, saldo: 305200, situacionSaldo: 'PENDIENTE' }
const SIMULACION = { retenido: 50000, saldo: 80800, situacionSaldo: 'A_FAVOR' }
const POLITICA = {
  tramos: [{ antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } }],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}
const CONGELADA = {
  tramos: [{ antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 20, montoFijo: null } }],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 30, montoFijo: null },
}
const PAGO_PENDIENTE = { montoAPagar: 130800, pagarAntesDe: '2027-03-01T15:30:00', condiciones: CONGELADA }
const PENDIENTE = { ...RESERVA, estado: 'PENDIENTE' }
const SIN_PAGOS = { totalPagado: 0, saldo: 436000, situacionSaldo: 'PENDIENTE' }
const PAGINA = (contenido: unknown[], totalPaginas = 1) =>
  ({ contenido, numero: 0, tamano: 10, totalElementos: contenido.length, totalPaginas })

type Ruta = (opciones: RequestInit) => Response

/** La API simulada: lo que no se indique responde como un hotel normal; devuelve el `fetch` para revisar llamadas. */
function laApi(rutas: Record<string, Ruta> = {}) {
  const fetch = vi.fn((ruta: string, opciones: RequestInit = {}) => {
    const propia = rutas[ruta]
    if (propia) return Promise.resolve(propia(opciones))
    if (ruta === '/api/alojamiento') {
      return Promise.resolve(respuesta({
        nombre: 'Hotel de Prueba',
        parametros: { umbralEdadFacturable: 12, horaEntrada: '15:00:00', horaSalida: '11:00:00' },
      }))
    }
    if (ruta.startsWith('/api/mis-reservas')) return Promise.resolve(respuesta(PAGINA([RESERVA])))
    if (ruta.endsWith('/folio')) return Promise.resolve(respuesta(FOLIO))
    if (ruta.endsWith('/cancelacion')) return Promise.resolve(respuesta(SIMULACION))
    if (ruta.endsWith('/pago-en-linea')) return Promise.resolve(respuesta(PAGO_PENDIENTE))
    if (ruta === '/api/portal/politica') return Promise.resolve(respuesta(POLITICA))
    if (ruta.startsWith('/api/reservas/')) return Promise.resolve(respuesta(RESERVA))
    if (ruta.startsWith('/api/portal/apartamentos/')) return Promise.resolve(respuesta(SUITE))
    return Promise.resolve(respuesta(PAGINA([])))
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

const llamadasA = (fetch: ReturnType<typeof laApi>, ruta: string) =>
  fetch.mock.calls.filter(([r]) => r === ruta).map(([, o]) => o as RequestInit)

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <App />
    </MemoryRouter>,
  )
}

async function pedirCodigo() {
  await userEvent.type(await screen.findByLabelText('Correo'), 'ana@correo.co')
  await userEvent.type(screen.getByLabelText('Número de documento'), '1094')
  await userEvent.click(screen.getByRole('button', { name: 'Enviarme un PIN' }))
  return screen.findByLabelText('PIN de 6 dígitos')
}

/** El botón se habilita cuando ya se sabe cuánto se retendría. */
async function confirmarCancelacion() {
  const boton = await screen.findByRole('button', { name: 'Sí, cancelar' })
  await waitFor(() => expect(boton).toBeEnabled())
  await userEvent.click(boton)
}

beforeEach(() => borrarSesion())
afterEach(() => {
  vi.unstubAllGlobals()
  borrarSesion()
})

describe('Entrar con PIN', () => {
  it('pide el PIN, entra y vuelve a la página que quería ver', async () => {
    const fetch = laApi({
      '/api/auth/huesped/pin': () => respuesta({ mensaje: 'Si los datos corresponden, le enviamos un PIN.' }, 202),
      '/api/auth/huesped/login': () => respuesta({ ...SESION, rol: 'HUESPED' }),
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.type(await pedirCodigo(), '123456')
    expect(screen.getByRole('status')).toHaveTextContent(/te enviamos un PIN a ana@correo\.co\. Vale por 15 minutos/)
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Suite Jardín' })).toBeInTheDocument()
    expect(JSON.parse(String(llamadasA(fetch, '/api/auth/huesped/login')[0].body)))
      .toEqual({ correo: 'ana@correo.co', numeroDocumento: '1094', pin: '123456' })
    expect(leerSesion()?.token).toBe('token-de-prueba')
  })

  it('un PIN equivocado muestra por qué, borra el campo y no entra', async () => {
    laApi({
      '/api/auth/huesped/pin': () => respuesta({ mensaje: 'Revisa tu correo.' }, 202),
      '/api/auth/huesped/login': () => errorApi(401, 'NO_AUTENTICADO', 'El PIN no es válido'),
    })
    abrir('/entrar')

    const campo = await pedirCodigo()
    await userEvent.type(campo, '000000')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El PIN no es válido')
    expect(campo).toHaveValue('')
    expect(leerSesion()).toBeNull()
  })

  it('el PIN solo admite dígitos y exige los 6 antes de enviarlo', async () => {
    const fetch = laApi({ '/api/auth/huesped/pin': () => respuesta({ mensaje: 'Revisa tu correo.' }, 202) })
    abrir('/entrar')

    const campo = await pedirCodigo()
    await userEvent.type(campo, '12a3')
    expect(campo).toHaveValue('123')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(llamadasA(fetch, '/api/auth/huesped/login')).toHaveLength(0)
  })

  it('un enlace armado no puede mandar a otro sitio después de entrar', async () => {
    laApi({
      '/api/auth/huesped/pin': () => respuesta({ mensaje: 'Revisa tu correo.' }, 202),
      '/api/auth/huesped/login': () => respuesta({ ...SESION, rol: 'HUESPED' }),
    })
    abrir('/entrar?volver=//sitio-falso.example')

    await userEvent.type(await pedirCodigo(), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Mis reservas' })).toBeInTheDocument()
  })
})

describe('Sesión', () => {
  it('sin sesión, las páginas del huésped llevan a entrar', async () => {
    laApi()
    abrir('/mi-cuenta')

    expect(await screen.findByRole('heading', { level: 1, name: 'Entra a tus reservas' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('una sesión vencida no cuenta', () => {
    guardarSesion({ ...SESION, expiraEn: new Date(Date.now() - 1000).toISOString() })

    expect(leerSesion()).toBeNull()
  })

  it('el token solo viaja en las llamadas del huésped, no en las públicas', async () => {
    guardarSesion(SESION)
    const fetch = laApi()
    abrir('/mis-reservas')
    await screen.findByText('Suite Jardín')

    expect(new Headers(llamadasA(fetch, '/api/mis-reservas?pagina=0&vigentes=true')[0].headers).get('Authorization'))
      .toBe('Bearer token-de-prueba')
    expect(new Headers(llamadasA(fetch, '/api/alojamiento')[0]?.headers).get('Authorization')).toBeNull()
    expect(new Headers(llamadasA(fetch, '/api/portal/apartamentos/APT-101')[0].headers).get('Authorization')).toBeNull()
  })

  it('si el backend rechaza el token, borra la sesión y pide entrar de nuevo', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mis-reservas?pagina=0&vigentes=true': () => errorApi(401, 'NO_AUTENTICADO', 'Inicie sesión') })
    abrir('/mis-reservas')

    expect(await screen.findByText('Tu sesión terminó. Vuelve a entrar para continuar.')).toBeInTheDocument()
    expect(leerSesion()).toBeNull()
  })

  it('salir cierra la sesión en el backend y aquí, aunque el backend no responda', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/auth/logout': () => errorApi(500, 'ERROR', 'Falló') })
    abrir('/mi-cuenta')

    await userEvent.click(await screen.findByRole('button', { name: 'Salir' }))

    expect(await screen.findByRole('link', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Entra a tus reservas' })).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/api/auth/logout')).toHaveLength(1)
    expect(leerSesion()).toBeNull()
  })
})

describe('Mi cuenta', () => {
  const CUENTA = {
    id: 'TIT-1', nombre: 'Ana Ríos', nombres: 'Ana', primerApellido: 'Ríos' as string | null, segundoApellido: null,
    tipoDocumento: 'CC', numeroDocumento: '1094', correo: 'ana@correo.co', telefono: '+573001234567',
    validado: false,
  }
  const cuerpo = (opciones: RequestInit) => JSON.parse(String(opciones.body)) as Record<string, unknown>
  /** GET devuelve la cuenta; PUT la guarda con lo que llegó, como el backend. */
  const conCuenta = (inicial: typeof CUENTA, alGuardar?: (o: RequestInit) => Response) => {
    let actual = { ...inicial }
    return (opciones: RequestInit) => {
      if (opciones.method !== 'PUT') return respuesta(actual)
      if (alGuardar) return alGuardar(opciones)
      const c = cuerpo(opciones)
      actual = { ...actual, ...c, nombre: [c.nombres, c.primerApellido, c.segundoApellido].filter(Boolean).join(' ') }
      return respuesta(actual)
    }
  }

  it('muestra los datos del titular', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mi-cuenta': conCuenta(CUENTA) })
    abrir('/mi-cuenta')

    expect(await screen.findByText('Cédula de ciudadanía 1094')).toBeInTheDocument()
    expect(screen.getByText('ana@correo.co')).toBeInTheDocument()
    expect(screen.getByText('+573001234567')).toBeInTheDocument()
  })

  it('sin validar, cambia sus nombres, sus apellidos y su teléfono', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/mi-cuenta': conCuenta(CUENTA) })
    abrir('/mi-cuenta')

    await userEvent.click(await screen.findByRole('button', { name: 'Editar mis datos' }))
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana')
    expect(screen.getByLabelText('Apellidos')).toHaveValue('Ríos')
    expect(screen.getByLabelText('Teléfono')).toHaveValue('3001234567')
    await userEvent.type(screen.getByLabelText('Nombres'), ' María')
    await userEvent.type(screen.getByLabelText('Apellidos'), ' Gómez')
    await userEvent.clear(screen.getByLabelText('Teléfono'))
    await userEvent.type(screen.getByLabelText('Teléfono'), '3112223333')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Tus datos quedaron guardados.')).toBeInTheDocument()
    expect(screen.getByText('Ana María Ríos Gómez')).toBeInTheDocument()
    const guardado = llamadasA(fetch, '/api/mi-cuenta').find((o) => o.method === 'PUT') as RequestInit
    expect(cuerpo(guardado)).toEqual({ nombres: 'Ana María', primerApellido: 'Ríos Gómez', telefono: '+573112223333' })
  })

  it('una cuenta de antes, con el nombre todo junto, pide separar los apellidos', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mi-cuenta': conCuenta({ ...CUENTA, nombres: 'Ana Ríos', primerApellido: null }) })
    abrir('/mi-cuenta')

    expect(await screen.findByText(/sepáralos en «Editar mis datos»/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Editar mis datos' }))
    expect(screen.getByRole('note')).toHaveTextContent('Tu nombre quedó guardado todo junto')
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana Ríos')
    expect(screen.getByLabelText('Apellidos')).toHaveValue('')
  })

  it('con la identidad validada solo cambia el teléfono y el nombre se pide en recepción', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/mi-cuenta': conCuenta({ ...CUENTA, validado: true }) })
    abrir('/mi-cuenta')

    expect(await screen.findByText(/pídelo en\s+recepción/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar mi teléfono' }))
    expect(screen.queryByLabelText('Nombres')).not.toBeInTheDocument()
    await userEvent.clear(screen.getByLabelText('Teléfono'))
    await userEvent.type(screen.getByLabelText('Teléfono'), '3112223333')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Tus datos quedaron guardados.')).toBeInTheDocument()
    const guardado = llamadasA(fetch, '/api/mi-cuenta').find((o) => o.method === 'PUT') as RequestInit
    expect(cuerpo(guardado)).toEqual({ nombres: 'Ana', primerApellido: 'Ríos', telefono: '+573112223333' })
  })

  it('si el backend rechaza el cambio, explica por qué y deja el formulario', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mi-cuenta': conCuenta(CUENTA, () => errorApi(409, 'REGLA_NEGOCIO',
      'El hotel ya validó tu identidad: para cambiar tu nombre, pídelo en recepción')) })
    abrir('/mi-cuenta')

    await userEvent.click(await screen.findByRole('button', { name: 'Editar mis datos' }))
    await userEvent.type(screen.getByLabelText('Nombres'), ' María')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('para cambiar tu nombre, pídelo en recepción')
    expect(screen.getByLabelText('Nombres')).toHaveValue('Ana María')
  })

  it('cambia el correo en dos pasos con el PIN que llega al correo actual', async () => {
    guardarSesion(SESION)
    let correo = 'ana@correo.co'
    const fetch = laApi({
      '/api/mi-cuenta': () => respuesta({ ...CUENTA, correo }),
      '/api/auth/huesped/cambio-correo': (opciones) => {
        if (opciones.method === 'POST') return respuesta({ mensaje: 'Si los datos corresponden…' }, 202)
        if (cuerpo(opciones).pin !== '123456') return errorApi(401, 'NO_AUTENTICADO', 'El PIN no es válido')
        correo = 'ana.nueva@correo.co'
        return respuesta({ mensaje: 'El correo de la cuenta quedó cambiado.' })
      },
    })
    abrir('/mi-cuenta')

    await userEvent.click(await screen.findByRole('button', { name: 'Cambiar mi correo' }))
    await userEvent.type(screen.getByLabelText('Correo nuevo'), 'ana.nueva@correo.co')
    await userEvent.click(screen.getByRole('button', { name: 'Enviarme un PIN' }))
    const pin = await screen.findByLabelText('PIN de 6 dígitos')
    await userEvent.type(pin, '000000')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar mi correo' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El PIN no es válido')
    expect(pin).toHaveValue('')
    await userEvent.type(pin, '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar mi correo' }))

    expect(await screen.findByText(/La próxima vez entra con ana\.nueva@correo\.co/)).toBeInTheDocument()
    const [pedido, primero, segundo] = llamadasA(fetch, '/api/auth/huesped/cambio-correo').map(cuerpo)
    expect(pedido).toEqual({ tipoDocumento: 'CC', numeroDocumento: '1094', nuevoCorreo: 'ana.nueva@correo.co' })
    expect(primero).toEqual({ tipoDocumento: 'CC', numeroDocumento: '1094', pin: '000000' })
    expect(segundo.pin).toBe('123456')
  })
})

describe('Mis reservas', () => {
  it('lista cada reserva con su estado, fechas y total con extras, y lleva a su detalle', async () => {
    guardarSesion(SESION)
    laApi()
    abrir('/mis-reservas')

    const fila = await screen.findByRole('link', { name: /Suite Jardín/ })
    expect(fila).toHaveAttribute('href', '/mis-reservas/RES-2026-00042')
    expect(fila).toHaveTextContent('Confirmada')
    expect(fila).toHaveTextContent('RES-2026-00042')
    expect(fila).toHaveTextContent(/\$\s*436\.000/)
  })

  it('si el nombre del apartamento no carga, muestra su código', async () => {
    guardarSesion(SESION)
    laApi({ '/api/portal/apartamentos/APT-101': () => errorApi(404, 'NO_ENCONTRADO', 'No existe') })
    abrir('/mis-reservas')

    expect(await screen.findByRole('link', { name: /APT-101/ })).toBeInTheDocument()
  })

  it('sin reservas próximas, invita a ver apartamentos', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mis-reservas?pagina=0&vigentes=true': () => respuesta(PAGINA([])) })
    abrir('/mis-reservas')

    expect(await screen.findByText('No tienes reservas próximas.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver apartamentos' })).toHaveAttribute('href', '/')
  })

  it('abre en las próximas y las anteriores, como las canceladas, van en su propia pestaña', async () => {
    guardarSesion(SESION)
    const CANCELADA = { ...RESERVA, codigo: 'RES-2026-00007', estado: 'CANCELADA' }
    const fetch = laApi({
      '/api/mis-reservas?pagina=0&vigentes=true': () => respuesta(PAGINA([RESERVA])),
      '/api/mis-reservas?pagina=0&vigentes=false': () => respuesta(PAGINA([CANCELADA])),
    })
    abrir('/mis-reservas')

    const proximas = await screen.findByRole('link', { name: 'Próximas' })
    expect(proximas).toHaveAttribute('aria-current', 'page')
    expect(await screen.findByText('RES-2026-00042')).toBeInTheDocument()
    expect(screen.queryByText('RES-2026-00007')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('link', { name: 'Anteriores' }))

    expect(await screen.findByText('RES-2026-00007')).toBeInTheDocument()
    expect(screen.getByText('Cancelada')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Anteriores' })).toHaveAttribute('aria-current', 'page')
    expect(llamadasA(fetch, '/api/mis-reservas?pagina=0&vigentes=false')).toHaveLength(1)
  })

  it('con más de una página, pide la siguiente sin salir de su pestaña', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/mis-reservas?pagina=0&vigentes=false': () => respuesta(PAGINA([RESERVA], 2)) })
    abrir('/mis-reservas?ver=anteriores')

    await userEvent.click(await screen.findByRole('button', { name: /Siguiente/ }))

    await waitFor(() => expect(llamadasA(fetch, '/api/mis-reservas?pagina=1&vigentes=false')).toHaveLength(1))
  })
})

describe('Detalle de una reserva', () => {
  it('muestra la estadía, los extras y cómo va la cuenta', async () => {
    guardarSesion(SESION)
    laApi()
    abrir('/mis-reservas/RES-2026-00042')

    const cuenta = await screen.findByRole('region', { name: 'Tu cuenta' })
    expect(within(cuenta).getByText('Parqueadero').nextElementSibling).toHaveTextContent(/\$\s*46\.000/)
    expect(within(cuenta).getByText('Pagado').nextElementSibling).toHaveTextContent(/\$\s*130\.800/)
    expect(within(cuenta).getByText('Falta por pagar').closest('dt')?.nextElementSibling)
      .toHaveTextContent(/\$\s*305\.200/)
    const estadia = screen.getByRole('region', { name: 'Tu estadía' })
    expect(estadia).toHaveTextContent('Ana Ríos, Luis Ríos')
    expect(within(estadia).getByText('Llegada').nextElementSibling).toHaveTextContent(/3:00/)
  })

  it('una reserva ajena o inexistente dice lo mismo', async () => {
    guardarSesion(SESION)
    laApi({ '/api/reservas/RES-2026-00042': () => errorApi(403, 'SIN_PERMISO', 'No puede ver esta reserva') })
    abrir('/mis-reservas/RES-2026-00042')

    expect(await screen.findByRole('heading', { level: 1, name: 'No encontramos esta reserva' })).toBeInTheDocument()
    expect(screen.queryByText('No puede ver esta reserva')).not.toBeInTheDocument()
  })

  it('cancelar pide confirmar y luego muestra lo retenido y lo que queda a favor', async () => {
    guardarSesion(SESION)
    let cancelada = false
    const fetch = laApi({
      '/api/reservas/RES-2026-00042/cancelar': () => {
        cancelada = true
        return respuesta({
          codigo: RESERVA.codigo, estado: 'CANCELADA', retenido: 50000, saldo: 80800, situacionSaldo: 'A_FAVOR',
        })
      },
      '/api/reservas/RES-2026-00042': () => respuesta({ ...RESERVA, estado: cancelada ? 'CANCELADA' : 'CONFIRMADA' }),
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))
    expect(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelar')).toHaveLength(0)
    await confirmarCancelacion()

    const resultado = await screen.findByText('Tu reserva quedó cancelada.')
    expect(resultado.closest('[role="status"]')).toHaveTextContent(/se retienen \$\s*50\.000/)
    expect(resultado.closest('[role="status"]')).toHaveTextContent(/\$\s*80\.800 a tu favor/)
    expect(screen.queryByRole('button', { name: 'Cancelar esta reserva' })).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelar')[0].method).toBe('PUT')
  })

  it('antes de cancelar dice cuánto se retendría y cuánto le devuelven, sin cancelar nada', async () => {
    guardarSesion(SESION)
    const fetch = laApi()
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))

    expect((await screen.findByText('Se retiene')).nextElementSibling).toHaveTextContent(/\$\s*50\.000/)
    expect(screen.getByText('Te devuelven').closest('dt')?.nextElementSibling).toHaveTextContent(/\$\s*80\.800/)
    expect(new Headers(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelacion')[0].headers).get('Authorization'))
      .toBe('Bearer token-de-prueba')
    expect(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelar')).toHaveLength(0)
  })

  it('si quedaría debiendo, también lo dice antes', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042/cancelacion': () => respuesta({
        retenido: 360000, saldo: 229200, situacionSaldo: 'PENDIENTE',
      }),
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))

    expect((await screen.findByText('Quedarías debiendo')).closest('dt')?.nextElementSibling)
      .toHaveTextContent(/\$\s*229\.200/)
  })

  it('si no se puede calcular lo retenido, no deja cancelar a ciegas', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042/cancelacion': () => errorApi(409, 'REGLA_NEGOCIO', 'La reserva ya inició'),
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('La reserva ya inició')
    expect(screen.getByRole('button', { name: 'Sí, cancelar' })).toBeDisabled()
  })

  it('una reserva pendiente se puede pagar desde su detalle, y queda confirmada', async () => {
    guardarSesion(SESION)
    let pagada = false
    const fetch = laApi({
      '/api/reservas/RES-2026-00042': () => respuesta(pagada ? RESERVA : PENDIENTE),
      '/api/reservas/RES-2026-00042/folio': () => respuesta(pagada ? FOLIO : SIN_PAGOS),
      '/api/portal/reservas/RES-2026-00042/pago': () => {
        pagada = true
        return respuesta({ codigo: RESERVA.codigo, estado: 'CONFIRMADA', total: 436000 })
      },
    })
    abrir('/mis-reservas/RES-2026-00042')

    const pago = await screen.findByRole('region', { name: 'Paga para confirmar' })
    expect(within(pago).getByText('Pagas ahora').nextElementSibling).toHaveTextContent(/\$\s*130\.800/)
    expect(within(pago).getByText('Pagas al llegar').nextElementSibling).toHaveTextContent(/\$\s*305\.200/)
    await userEvent.click(within(pago).getByRole('button', { name: /^Pagar/ }))

    expect(await screen.findByText('Recibimos tu pago. Tu reserva quedó confirmada.')).toBeInTheDocument()
    expect(await screen.findByText('Confirmada')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Paga para confirmar' })).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/api/portal/reservas/RES-2026-00042/pago')).toHaveLength(1)
    // DEC-93: desde «mis reservas» no hay clave de la reserva: paga con la sesión del huésped
    expect(llamadasA(fetch, '/api/portal/reservas/RES-2026-00042/pago')[0].headers)
      .toMatchObject({ Authorization: 'Bearer token-de-prueba' })
  })

  it('después de pagar y cancelar, solo queda el aviso de la cancelación', async () => {
    guardarSesion(SESION)
    let estado = 'PENDIENTE'
    laApi({
      '/api/reservas/RES-2026-00042': () => respuesta({ ...RESERVA, estado }),
      '/api/portal/reservas/RES-2026-00042/pago': () => {
        estado = 'CONFIRMADA'
        return respuesta({ codigo: RESERVA.codigo, estado, total: 436000 })
      },
      '/api/reservas/RES-2026-00042/cancelar': () => {
        estado = 'CANCELADA'
        return respuesta({ codigo: RESERVA.codigo, estado, ...SIMULACION })
      },
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(within(await screen.findByRole('region', { name: 'Paga para confirmar' }))
      .getByRole('button', { name: /^Pagar/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))
    await confirmarCancelacion()

    expect(await screen.findByText('Tu reserva quedó cancelada.')).toBeInTheDocument()
    expect(screen.queryByText('Recibimos tu pago. Tu reserva quedó confirmada.')).not.toBeInTheDocument()
    expect(screen.getByText(/ya no se cobran/)).toBeInTheDocument()
  })

  it('al retomar el pago muestra las condiciones congeladas en la reserva, no las vigentes', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/reservas/RES-2026-00042': () => respuesta(PENDIENTE) })
    abrir('/mis-reservas/RES-2026-00042')

    const condiciones = await screen.findByRole('region', { name: 'Si necesitas cancelar' })
    expect(condiciones).toHaveTextContent('se retiene el 20 %')
    expect(condiciones).toHaveTextContent('se retiene el 30 %')
    expect(condiciones).not.toHaveTextContent('se retiene el 50 %')
    expect(llamadasA(fetch, '/api/portal/politica')).toHaveLength(0)
  })

  it('al retomar el pago, lo que falta al llegar descuenta lo ya abonado', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042': () => respuesta(PENDIENTE),
      '/api/reservas/RES-2026-00042/folio': () => respuesta({ ...SIN_PAGOS, totalPagado: 30000 }),
      '/api/reservas/RES-2026-00042/pago-en-linea': () => respuesta({ ...PAGO_PENDIENTE, montoAPagar: 100800 }),
    })
    abrir('/mis-reservas/RES-2026-00042')

    const pago = await screen.findByRole('region', { name: 'Paga para confirmar' })
    expect(within(pago).getByText('Pagas al llegar').nextElementSibling).toHaveTextContent(/\$\s*305\.200/)
  })

  it('un pago rechazado se explica y se puede reintentar', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042': () => respuesta(PENDIENTE),
      '/api/reservas/RES-2026-00042/folio': () => respuesta(SIN_PAGOS),
      '/api/portal/reservas/RES-2026-00042/pago': () => errorApi(409, 'PAGO_RECHAZADO', 'El banco rechazó el pago'),
    })
    abrir('/mis-reservas/RES-2026-00042')

    const pago = await screen.findByRole('region', { name: 'Paga para confirmar' })
    await userEvent.click(within(pago).getByRole('button', { name: /^Pagar/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El banco rechazó el pago')
    expect(within(pago).getByRole('button', { name: /^Pagar/ })).toBeEnabled()
  })

  it('si el plazo para pagar ya venció, lo dice y no ofrece pagar', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042': () => respuesta(PENDIENTE),
      '/api/reservas/RES-2026-00042/pago-en-linea': () =>
        errorApi(409, 'REGLA_NEGOCIO', 'El plazo para pagarla terminó'),
    })
    abrir('/mis-reservas/RES-2026-00042')

    expect(await screen.findByText('El plazo para pagarla terminó')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Pagar/ })).not.toBeInTheDocument()
  })

  it('«No, conservarla» no cancela', async () => {
    guardarSesion(SESION)
    const fetch = laApi()
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))
    await userEvent.click(screen.getByRole('button', { name: 'No, conservarla' }))

    expect(screen.getByRole('button', { name: 'Cancelar esta reserva' })).toBeInTheDocument()
    expect(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelar')).toHaveLength(0)
  })

  it('si cancelar falla, explica por qué y deja intentar de nuevo', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/reservas/RES-2026-00042/cancelar': () => errorApi(409, 'REGLA_NEGOCIO', 'La reserva ya inició'),
    })
    abrir('/mis-reservas/RES-2026-00042')

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar esta reserva' }))
    await confirmarCancelacion()

    expect(await screen.findByRole('alert')).toHaveTextContent('La reserva ya inició')
    expect(screen.getByRole('button', { name: 'Sí, cancelar' })).toBeEnabled()
  })

  it.each(['EN_CURSO', 'FINALIZADA', 'CANCELADA', 'NO_SHOW'])('una reserva %s no se puede cancelar', async (estado) => {
    guardarSesion(SESION)
    laApi({ '/api/reservas/RES-2026-00042': () => respuesta({ ...RESERVA, estado }) })
    abrir('/mis-reservas/RES-2026-00042')

    await screen.findByRole('region', { name: 'Tu cuenta' })
    expect(screen.queryByRole('button', { name: 'Cancelar esta reserva' })).not.toBeInTheDocument()
  })
})
