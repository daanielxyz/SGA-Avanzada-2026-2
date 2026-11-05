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
    expect(screen.getByRole('status')).toHaveTextContent('te enviamos un PIN a ana@correo.co')
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

    expect(new Headers(llamadasA(fetch, '/api/mis-reservas?pagina=0')[0].headers).get('Authorization'))
      .toBe('Bearer token-de-prueba')
    expect(new Headers(llamadasA(fetch, '/api/alojamiento')[0]?.headers).get('Authorization')).toBeNull()
    expect(new Headers(llamadasA(fetch, '/api/portal/apartamentos/APT-101')[0].headers).get('Authorization')).toBeNull()
  })

  it('si el backend rechaza el token, borra la sesión y pide entrar de nuevo', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mis-reservas?pagina=0': () => errorApi(401, 'NO_AUTENTICADO', 'Inicie sesión') })
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
  it('muestra los datos del titular', async () => {
    guardarSesion(SESION)
    laApi({
      '/api/mi-cuenta': () => respuesta({
        id: 'TIT-1', nombre: 'Ana Ríos', tipoDocumento: 'CC', numeroDocumento: '1094', correo: 'ana@correo.co',
        telefono: '+573001234567',
      }),
    })
    abrir('/mi-cuenta')

    expect(await screen.findByText('Cédula de ciudadanía 1094')).toBeInTheDocument()
    expect(screen.getByText('ana@correo.co')).toBeInTheDocument()
    expect(screen.getByText('+573001234567')).toBeInTheDocument()
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

  it('sin reservas, invita a ver apartamentos', async () => {
    guardarSesion(SESION)
    laApi({ '/api/mis-reservas?pagina=0': () => respuesta(PAGINA([])) })
    abrir('/mis-reservas')

    expect(await screen.findByText('Todavía no tienes reservas.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver apartamentos' })).toHaveAttribute('href', '/')
  })

  it('con más de una página, pide la siguiente', async () => {
    guardarSesion(SESION)
    const fetch = laApi({ '/api/mis-reservas?pagina=0': () => respuesta(PAGINA([RESERVA], 2)) })
    abrir('/mis-reservas')

    await userEvent.click(await screen.findByRole('button', { name: /Siguiente/ }))

    await waitFor(() => expect(llamadasA(fetch, '/api/mis-reservas?pagina=1')).toHaveLength(1))
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
    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

    const resultado = await screen.findByText('Tu reserva quedó cancelada.')
    expect(resultado.closest('[role="status"]')).toHaveTextContent(/se retienen \$\s*50\.000/)
    expect(resultado.closest('[role="status"]')).toHaveTextContent(/\$\s*80\.800 a tu favor/)
    expect(screen.queryByRole('button', { name: 'Cancelar esta reserva' })).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/api/reservas/RES-2026-00042/cancelar')[0].method).toBe('PUT')
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
    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar' }))

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
