import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Folio } from '../api/folio'
import type { Reserva } from '../api/reservas'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RUTA = '/api/reservas/RES-2026-00042'
const TOTAL = 1250000
const ANTICIPO = 375000

function folioDePrueba(cambios: Partial<Folio> = {}): Folio {
  return {
    id: 'FOL-1',
    reserva: 'RES-2026-00042',
    cargos: [{ id: 'C-1', tipo: 'HOSPEDAJE', concepto: 'Alojamiento', valor: TOTAL, sentido: null, fecha: '2026-11-01' }],
    totalPagado: 0,
    saldo: TOTAL,
    situacionSaldo: 'PENDIENTE',
    cerrado: false,
    ...cambios,
  }
}

function alojamientoDePrueba(anticipoPct = 30) {
  return {
    nombre: 'Puerta al Sol',
    parametros: { anticipoPct, horaLimiteNoShow: '22:00:00' },
    mediosPago: ['EFECTIVO', 'TRANSFERENCIA', 'PAGO_EN_LINEA'],
  }
}

function abrir() {
  entrarComo('RECEPCIONISTA')
  render(
    <MemoryRouter initialEntries={['/reservas/RES-2026-00042']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
}

/** API simulada que recuerda lo que cambia, como la de verdad. */
const PREVIA = { retenido: 125000, saldo: 250000, situacionSaldo: 'A_FAVOR' }

function apiCon({ reserva, folio = folioDePrueba(), anticipoPct = 30, rechazar = {}, previa = () => [200, PREVIA] }: {
  reserva: Partial<Reserva>
  folio?: Folio
  anticipoPct?: number
  rechazar?: Record<string, string>
  /** Lo que responde `GET …/cancelacion` (DEC-91). */
  previa?: () => [number, unknown]
}) {
  // Como el backend: el detalle trae el porcentaje de la estancia más los extras (DEC-95)
  const base = reservaDePrueba(reserva)
  const estado = {
    reserva: {
      ...base,
      anticipoExigido: reserva.anticipoExigido ?? Math.round(((base.valor.total + base.extras.total) * anticipoPct) / 100),
    },
    folio,
  }
  const rechazo = (mensaje: string): [number, unknown] => [
    409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje, detalles: [] },
  ]
  const accion = (ruta: string, hacer: () => unknown) => (): [number, unknown] =>
    rechazar[ruta] ? rechazo(rechazar[ruta]) : [200, hacer()]
  const liquidar = (nuevo: Reserva['estado']) => () => {
    estado.reserva = { ...estado.reserva, estado: nuevo }
    estado.folio = folioDePrueba({ totalPagado: 375000, saldo: 250000, situacionSaldo: 'A_FAVOR', cargos: [{
      id: 'C-2', tipo: 'PENALIDAD_CANCELACION', concepto: 'Penalidad', valor: 125000, sentido: null, fecha: '2026-11-02',
    }] })
    return { codigo: estado.reserva.codigo, estado: nuevo, retenido: 125000, saldo: 250000, situacionSaldo: 'A_FAVOR' }
  }
  return laApiRespondePorRuta({
    [RUTA]: () => [200, estado.reserva],
    [`${RUTA}/folio`]: () => [200, estado.folio],
    '/api/alojamiento': [200, alojamientoDePrueba(anticipoPct)],
    '/api/apartamentos/APT-101': [200, { codigo: 'APT-101', nombre: 'Penthouse Mirador' }],
    [`${RUTA}/hora-llegada`]: accion('hora-llegada', () => {
      estado.reserva = { ...estado.reserva, horaEstimadaLlegada: '15:00:00' }
      return estado.reserva
    }),
    [`${RUTA}/confirmar`]: accion('confirmar', () => {
      estado.reserva = { ...estado.reserva, estado: 'CONFIRMADA' }
      return estado.reserva
    }),
    [`${RUTA}/cancelacion`]: previa,
    [`${RUTA}/cancelar`]: accion('cancelar', liquidar('CANCELADA')),
    [`${RUTA}/no-show`]: accion('no-show', liquidar('NO_SHOW')),
    [`${RUTA}/folio/pagos`]: accion('pagos', () => {
      estado.folio = folioDePrueba({ totalPagado: ANTICIPO, saldo: TOTAL - ANTICIPO })
      return estado.folio
    }),
  })
}

function llamadasA(fetch: ReturnType<typeof apiCon>, sufijo: string) {
  return fetch.mock.calls.filter(([url]) => String(url).endsWith(sufijo))
}

const PENDIENTE = { estado: 'PENDIENTE' as const, horaEstimadaLlegada: '15:00:00' }
const CONFIRMADA_VENCIDA = { estado: 'CONFIRMADA' as const, entrada: '2020-01-01', salida: '2020-01-03' }

describe('botones según el estado (RN-08)', () => {
  it('una reserva pendiente se puede confirmar o cancelar', async () => {
    apiCon({ reserva: PENDIENTE })
    abrir()

    expect(await screen.findByRole('button', { name: 'Confirmar' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeEnabled()
    expect(screen.queryByRole('button', { name: 'Declarar no-show' })).not.toBeInTheDocument()
  })

  it('antes de la hora límite el no-show está deshabilitado y dice desde cuándo (RES-16)', async () => {
    apiCon({ reserva: { estado: 'CONFIRMADA', entrada: '2099-11-14', salida: '2099-11-16' } })
    abrir()

    await screen.findByText('Disponible desde 14 nov, 10:00 p. m.')
    expect(screen.getByRole('button', { name: 'Declarar no-show' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Declarar no-show' })).toHaveAccessibleDescription(
      'Disponible desde 14 nov, 10:00 p. m.',
    )
  })

  it('pasada la hora límite el no-show se puede declarar', async () => {
    apiCon({ reserva: CONFIRMADA_VENCIDA })
    abrir()

    await screen.findByRole('heading', { name: 'Datos de la reserva' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Declarar no-show' })).toBeEnabled())
  })

  it('una reserva en curso solo registra la salida y abre su folio (SAL-01)', async () => {
    apiCon({ reserva: { estado: 'EN_CURSO' } })
    abrir()

    expect(await screen.findByRole('button', { name: 'Registrar salida' })).toBeEnabled()
    expect(await screen.findByRole('link', { name: /Ver folio/ })).toHaveAttribute('href', '/reservas/RES-2026-00042/folio')
    for (const nombre of ['Confirmar', 'Cancelar', 'Declarar no-show']) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument()
    }
  })

  it.each(['FINALIZADA', 'CANCELADA', 'NO_SHOW'] as const)('%s no tiene acciones', async (estado) => {
    apiCon({ reserva: { estado } })
    abrir()

    await screen.findByRole('heading', { name: 'Datos de la reserva' })
    for (const nombre of ['Confirmar', 'Cancelar', 'Declarar no-show', 'Registrar salida']) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument()
    }
  })
})

describe('bloque Pago (RN-15 · RES-15)', () => {
  it('muestra el anticipo que falta', async () => {
    apiCon({ reserva: PENDIENTE })
    abrir()

    expect(await screen.findByText('Saldo pendiente')).toBeInTheDocument()
    expect(screen.getByText('Anticipo exigido').nextSibling).toHaveTextContent('$ 375.000')
    expect(screen.getByText(/Falta para el anticipo/)).toHaveTextContent('$ 375.000')
  })

  it('usa el anticipo que manda la API, sin calcularlo (DEC-95)', async () => {
    apiCon({ reserva: { ...PENDIENTE, anticipoExigido: 999000 } })
    abrir()

    expect((await screen.findByText('Anticipo exigido')).nextSibling).toHaveTextContent('$ 999.000')
    expect(screen.getByText(/Falta para el anticipo/)).toHaveTextContent('$ 999.000')
  })

  it('dice si el anticipo está cubierto', async () => {
    apiCon({ reserva: PENDIENTE, folio: folioDePrueba({ totalPagado: ANTICIPO, saldo: TOTAL - ANTICIPO }) })
    abrir()

    expect(await screen.findByText('Anticipo cubierto')).toBeInTheDocument()
  })

  it('si el hotel no exige anticipo, esa línea no aparece', async () => {
    apiCon({ reserva: PENDIENTE, anticipoPct: 0 })
    abrir()

    await screen.findByText('Saldo pendiente')
    expect(screen.queryByText('Anticipo exigido')).not.toBeInTheDocument()
    expect(screen.queryByText(/anticipo/i)).not.toBeInTheDocument()
  })

  it('un saldo a favor lo dice en su título', async () => {
    apiCon({ reserva: PENDIENTE, folio: folioDePrueba({ totalPagado: TOTAL + 5000, saldo: 5000, situacionSaldo: 'A_FAVOR' }) })
    abrir()

    expect((await screen.findByText('Saldo a favor')).nextSibling).toHaveTextContent('$ 5.000')
  })

  it('el anticipo incluye el parqueadero y las mascotas, pero no los consumos (DEC-89)', async () => {
    apiCon({
      reserva: { ...PENDIENTE, extras: { parqueadero: 60000, mascotas: 30000, total: 90000 } },
      folio: folioDePrueba({
        saldo: TOTAL + 90000 + 8000,
        cargos: [
          { id: 'C-1', tipo: 'HOSPEDAJE', concepto: 'Alojamiento', valor: TOTAL, sentido: null, fecha: '2026-11-01' },
          { id: 'C-2', tipo: 'SERVICIO_ADICIONAL', concepto: 'Parqueadero', valor: 60000, sentido: null, fecha: '2026-11-01' },
          { id: 'C-3', tipo: 'SERVICIO_ADICIONAL', concepto: 'Mascotas', valor: 30000, sentido: null, fecha: '2026-11-01' },
          { id: 'C-4', tipo: 'SERVICIO_ADICIONAL', concepto: 'Agua', valor: 8000, sentido: null, fecha: '2026-11-02' },
        ],
      }),
    })
    abrir()

    // 30 % de 1.250.000 + 90.000
    expect((await screen.findByText('Anticipo exigido')).nextSibling).toHaveTextContent('$ 402.000')
    expect(screen.getByText(/Falta para el anticipo/)).toHaveTextContent('$ 402.000')
    expect(screen.getByText('Estancia').nextSibling).toHaveTextContent('$ 1.250.000')
    expect(screen.getByText('Parqueadero').nextSibling).toHaveTextContent('$ 60.000')
    expect(screen.getByText('Cargo por mascotas').nextSibling).toHaveTextContent('$ 30.000')
    expect(screen.getByText('Consumos').nextSibling).toHaveTextContent('$ 8.000')
  })

  it('en una reserva cancelada los extras salen como anulados (DEC-89)', async () => {
    apiCon({
      reserva: { estado: 'CANCELADA', extras: { parqueadero: 60000, mascotas: 0, total: 60000 } },
      folio: folioDePrueba({ saldo: 0, totalPagado: 0, situacionSaldo: 'AL_DIA' }),
    })
    abrir()

    expect((await screen.findByText('Parqueadero')).nextSibling).toHaveTextContent('$ 60.000 · anulado')
  })

  it('en una reserva vigente los extras no dicen anulado', async () => {
    apiCon({ reserva: { ...PENDIENTE, extras: { parqueadero: 60000, mascotas: 0, total: 60000 } } })
    abrir()

    expect((await screen.findByText('Parqueadero')).nextSibling).toHaveTextContent(/^\$ 60\.000$/)
  })

  it('las mascotas cobradas antes de DEC-89 no se cuentan como consumos', async () => {
    apiCon({
      reserva: { ...PENDIENTE, extras: { parqueadero: 0, mascotas: 30000, total: 30000 } },
      folio: folioDePrueba({
        saldo: TOTAL + 30000,
        cargos: [
          { id: 'C-1', tipo: 'HOSPEDAJE', concepto: 'Alojamiento', valor: TOTAL, sentido: null, fecha: '2026-11-01' },
          { id: 'C-2', tipo: 'SERVICIO_ADICIONAL', concepto: 'Mascotas (1)', valor: 30000, sentido: null, fecha: '2026-11-01' },
        ],
      }),
    })
    abrir()

    expect((await screen.findByText('Cargo por mascotas')).nextSibling).toHaveTextContent('$ 30.000')
    expect(screen.queryByText('Consumos')).not.toBeInTheDocument()
  })

  it('una reserva terminada con saldo a favor no recibe pagos y muestra lo retenido', async () => {
    apiCon({
      reserva: { estado: 'CANCELADA' },
      folio: folioDePrueba({ saldo: 250000, totalPagado: 375000, situacionSaldo: 'A_FAVOR', cargos: [{
        id: 'C-2', tipo: 'PENALIDAD_CANCELACION', concepto: 'Penalidad', valor: 125000, sentido: null, fecha: '2026-11-02',
      }] }),
    })
    abrir()

    expect((await screen.findByText('Valor retenido')).nextSibling).toHaveTextContent('$ 125.000')
    expect(screen.queryByRole('button', { name: 'Registrar pago' })).not.toBeInTheDocument()
    expect(screen.queryByText('Anticipo exigido')).not.toBeInTheDocument()
  })

  it('una reserva terminada que aún debe sí puede pagar lo retenido', async () => {
    apiCon({ reserva: { estado: 'CANCELADA' }, folio: folioDePrueba({ saldo: 125000 }) })
    abrir()

    expect(await screen.findByRole('button', { name: 'Registrar pago' })).toBeInTheDocument()
  })

  it('si falla se explica y se puede reintentar', async () => {
    let falla = true
    laApiRespondePorRuta({
      [RUTA]: [200, reservaDePrueba(PENDIENTE)],
      '/api/alojamiento': [200, alojamientoDePrueba()],
      [`${RUTA}/folio`]: () => (falla
        ? [500, { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'Algo falló en el servidor', detalles: [] }]
        : [200, folioDePrueba()]),
    })
    abrir()
    const usuario = userEvent.setup()

    const error = await screen.findByRole('alert')
    expect(error).toHaveTextContent('No pudimos cargar el pago')
    expect(screen.getByRole('heading', { name: 'Datos de la reserva' })).toBeInTheDocument()

    falla = false
    await usuario.click(within(error).getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('Saldo pendiente')).toBeInTheDocument()
  })
})

describe('registrar pago (PAG-02 · PAG-07)', () => {
  async function abrirPago() {
    const usuario = userEvent.setup()
    await usuario.click(await screen.findByRole('button', { name: 'Registrar pago' }))
    return { usuario, dialogo: await screen.findByRole('dialog', { name: 'Registrar pago' }) }
  }

  it('los medios salen del alojamiento, sin el pago en línea', async () => {
    apiCon({ reserva: PENDIENTE })
    abrir()
    const { dialogo } = await abrirPago()

    const medios = within(within(dialogo).getByLabelText('Medio de pago')).getAllByRole('option')
    expect(medios.map((medio) => medio.textContent)).toEqual(['Efectivo', 'Transferencia'])
  })

  it('registra el pago y actualiza el bloque', async () => {
    const fetch = apiCon({ reserva: PENDIENTE })
    abrir()
    const { usuario, dialogo } = await abrirPago()

    expect(within(dialogo).getByLabelText('Monto (COP)')).toHaveValue('375.000')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))

    expect(await screen.findByText('Pago registrado')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('Anticipo cubierto')).toBeInTheDocument()
    const [, opciones] = llamadasA(fetch, '/folio/pagos')[0] as [string, RequestInit]
    expect(JSON.parse(String(opciones.body))).toMatchObject({ medio: 'EFECTIVO', monto: ANTICIPO })
  })

  it('un monto en cero se marca junto al campo y no se envía', async () => {
    const fetch = apiCon({ reserva: PENDIENTE })
    abrir()
    const { usuario, dialogo } = await abrirPago()

    await usuario.clear(within(dialogo).getByLabelText('Monto (COP)'))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))

    expect(within(dialogo).getByLabelText('Monto (COP)')).toBeInvalid()
    expect(within(dialogo).getByText('Escribe un monto mayor que $ 0.')).toBeInTheDocument()
    expect(llamadasA(fetch, '/folio/pagos')).toHaveLength(0)
  })

  it('una fecha futura se marca junto al campo y no se envía (PAG-07)', async () => {
    const fetch = apiCon({ reserva: PENDIENTE })
    abrir()
    const { usuario, dialogo } = await abrirPago()

    fireEvent.change(within(dialogo).getByLabelText('Fecha'), { target: { value: '2099-01-01' } })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))

    expect(within(dialogo).getByText(/La fecha no puede ser posterior a hoy/)).toBeInTheDocument()
    expect(llamadasA(fetch, '/folio/pagos')).toHaveLength(0)
  })

  it('si el backend lo rechaza, muestra su mensaje sin perder lo escrito (409)', async () => {
    apiCon({ reserva: PENDIENTE, rechazar: { pagos: 'El folio FOL-1 está cerrado: no admite movimientos' } })
    abrir()
    const { usuario, dialogo } = await abrirPago()

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('está cerrado')
    expect(within(dialogo).getByLabelText('Monto (COP)')).toHaveValue('375.000')
  })
})

describe('confirmar (RN-08 · RN-09 · RES-15)', () => {
  async function abrirConfirmar() {
    const usuario = userEvent.setup()
    await screen.findByText('Saldo pendiente')
    await usuario.click(screen.getByRole('button', { name: 'Confirmar' }))
    return { usuario, dialogo: await screen.findByRole('dialog', { name: 'Confirmar reserva' }) }
  }

  it('con la hora y el anticipo listos, confirma y deja el aviso en el detalle', async () => {
    const fetch = apiCon({ reserva: PENDIENTE, folio: folioDePrueba({ totalPagado: ANTICIPO, saldo: TOTAL - ANTICIPO }) })
    abrir()
    const { usuario, dialogo } = await abrirConfirmar()

    expect(within(dialogo).getByText('Ana Ríos')).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByText('La reserva quedó confirmada')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Declarar no-show' })).toBeInTheDocument()
    expect(llamadasA(fetch, '/hora-llegada')).toHaveLength(0)
    expect(screen.getByRole('heading', { level: 1 })).toHaveFocus()
  })

  it('sin hora no deja confirmar; al escribirla la guarda antes de confirmar (RN-09)', async () => {
    const fetch = apiCon({
      reserva: { estado: 'PENDIENTE', horaEstimadaLlegada: null },
      folio: folioDePrueba({ totalPagado: ANTICIPO, saldo: TOTAL - ANTICIPO }),
    })
    abrir()
    const { usuario, dialogo } = await abrirConfirmar()

    const boton = within(dialogo).getByRole('button', { name: 'Confirmar reserva' })
    expect(boton).toBeDisabled()
    fireEvent.change(within(dialogo).getByLabelText(/Hora estimada de llegada/), { target: { value: '15:00' } })
    await usuario.click(boton)

    await screen.findByText('La reserva quedó confirmada')
    const urls = fetch.mock.calls.map(([url]) => String(url))
    expect(urls.findIndex((url) => url.endsWith('/hora-llegada'))).toBeLessThan(
      urls.findIndex((url) => url.endsWith('/confirmar')),
    )
  })

  it('sin anticipo avisa cuánto falta, lo registra ahí mismo y vuelve a confirmar (RES-15)', async () => {
    apiCon({ reserva: PENDIENTE })
    abrir()
    const { usuario, dialogo } = await abrirConfirmar()

    expect(within(dialogo).getByText('Faltan $ 375.000 para confirmar.')).toBeInTheDocument()
    expect(within(dialogo).getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))
    const pago = await screen.findByRole('dialog', { name: 'Registrar pago' })
    expect(within(pago).getByLabelText('Monto (COP)')).toHaveValue('375.000')
    await usuario.click(within(pago).getByRole('button', { name: 'Registrar pago' }))

    const devuelta = await screen.findByRole('dialog', { name: 'Confirmar reserva' })
    expect(within(devuelta).getByText('El anticipo quedó cubierto.')).toBeInTheDocument()
    expect(within(devuelta).getByRole('button', { name: 'Confirmar reserva' })).toBeEnabled()
  })

  it('si el backend lo rechaza, muestra su mensaje y lee de nuevo la reserva (409)', async () => {
    const fetch = apiCon({
      reserva: PENDIENTE,
      folio: folioDePrueba({ totalPagado: ANTICIPO, saldo: TOTAL - ANTICIPO }),
      rechazar: { confirmar: 'Transición de reserva no permitida: CANCELADA → CONFIRMADA' },
    })
    abrir()
    const { usuario, dialogo } = await abrirConfirmar()
    const lecturas = llamadasA(fetch, RUTA).length

    await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar reserva' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Transición de reserva no permitida')
    await waitFor(() => expect(llamadasA(fetch, RUTA).length).toBeGreaterThan(lecturas))
  })

  it('Escape cierra la ventana sin confirmar', async () => {
    const fetch = apiCon({ reserva: PENDIENTE })
    abrir()
    const { dialogo } = await abrirConfirmar()

    fireEvent(dialogo, new Event('cancel', { cancelable: true }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(llamadasA(fetch, '/confirmar')).toHaveLength(0)
    expect(screen.getByRole('button', { name: 'Confirmar' })).toHaveFocus()
  })
})

describe('cancelar y no-show (RN-08 · RN-13)', () => {
  it('avisa antes de cancelar y al terminar muestra lo retenido', async () => {
    apiCon({ reserva: PENDIENTE })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })
    expect(within(dialogo).getByText(/No se puede deshacer/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/del 14 nov 2026 al 16 nov 2026/)).toBeInTheDocument()
    // Antes de decidir, cuánto se retendría (DEC-91)
    expect((await within(dialogo).findByText('Se retendrá')).nextSibling).toHaveTextContent('$ 125.000')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar reserva' }))

    const resultado = await screen.findByRole('dialog', { name: 'Reserva cancelada' })
    expect(within(resultado).getByText('Valor retenido').nextSibling).toHaveTextContent('$ 125.000')
    expect(within(resultado).getByText('Situación').nextSibling).toHaveTextContent('A favor')

    const [, cerrar] = within(resultado).getAllByRole('button', { name: 'Cerrar' })
    await usuario.click(cerrar)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText(/Se retuvo \$ 125\.000/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toHaveFocus()
  })

  it('sin la retención no deja cancelar: avisa y deja reintentar (DEC-91)', async () => {
    let responder = false
    apiCon({ reserva: PENDIENTE, previa: () => (responder ? [200, PREVIA] : [503, { estado: 503, codigo: 'ERROR_INTERNO', mensaje: 'Falló', detalles: [] }]) })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })
    // Si falla, avisa y deja reintentar; sin el valor no se cancela, con él sí
    const error = await within(dialogo).findByRole('alert')
    expect(error).toHaveTextContent('No pudimos calcular la retención')
    expect(error).toHaveTextContent('Sin ese valor no se puede continuar.')
    expect(within(dialogo).getByRole('button', { name: 'Cancelar reserva' })).toBeDisabled()
    responder = true
    await usuario.click(within(dialogo).getByRole('button', { name: 'Reintentar' }))

    expect((await within(dialogo).findByText('Se retendrá')).nextSibling).toHaveTextContent('$ 125.000')
    expect(within(dialogo).getByRole('button', { name: 'Cancelar reserva' })).toBeEnabled()
  })

  it('si no hay retención lo dice (DEC-91)', async () => {
    apiCon({ reserva: PENDIENTE, previa: () => [200, { retenido: 0, saldo: 0, situacionSaldo: 'AL_DIA' }] })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })

    expect(await within(dialogo).findByText('No se retiene nada')).toBeInTheDocument()
    expect(within(dialogo).queryByText('Se retendrá')).not.toBeInTheDocument()
  })

  it('si la reserva ya no se puede cancelar, muestra el motivo sin «Reintentar» (409)', async () => {
    apiCon({
      reserva: PENDIENTE,
      previa: () => [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La reserva ya está EN_CURSO y no se puede cancelar', detalles: [] }],
    })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('ya está EN_CURSO')
    expect(within(dialogo).queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
    expect(within(dialogo).getByRole('button', { name: 'Cancelar reserva' })).toBeDisabled()
  })

  it('si otro turno ya cambió la reserva, lo dice en palabras de recepción (DEC-94)', async () => {
    apiCon({
      reserva: PENDIENTE,
      previa: () => [409, {
        estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Transición de reserva no permitida: CANCELADA → CANCELADA',
        detalles: [], motivo: 'ESTADO_NO_PERMITIDO',
      }],
    })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })

    const aviso = await within(dialogo).findByRole('alert')
    expect(aviso).toHaveTextContent('Esta reserva ya cambió de estado')
    expect(aviso).not.toHaveTextContent('Transición')
  })

  it('«Volver» no cancela', async () => {
    const fetch = apiCon({ reserva: PENDIENTE })
    abrir()
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cancelar reserva' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Volver' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(llamadasA(fetch, '/cancelar')).toHaveLength(0)
  })

  it('declara el no-show y el detalle queda sin acciones', async () => {
    const fetch = apiCon({ reserva: CONFIRMADA_VENCIDA })
    abrir()
    const usuario = userEvent.setup()

    await waitFor(() => expect(screen.getByRole('button', { name: 'Declarar no-show' })).toBeEnabled())
    await usuario.click(screen.getByRole('button', { name: 'Declarar no-show' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Declarar no-show' })
    expect(within(dialogo).getByText(/queda como «No se presentó»/)).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Declarar no-show' }))

    await screen.findByRole('dialog', { name: 'No-show declarado' })
    expect(screen.getAllByText('No se presentó').length).toBeGreaterThan(0)
    // El no-show todavía no tiene su cálculo previo en la API (#1b): no se consulta el de cancelar
    expect(llamadasA(fetch, '/cancelacion')).toHaveLength(0)
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })

  it('si el backend rechaza el no-show, muestra su mensaje (409)', async () => {
    apiCon({ reserva: CONFIRMADA_VENCIDA, rechazar: { 'no-show': 'El no-show solo puede declararse desde 2020-01-01T22:00' } })
    abrir()
    const usuario = userEvent.setup()

    await waitFor(() => expect(screen.getByRole('button', { name: 'Declarar no-show' })).toBeEnabled())
    await usuario.click(screen.getByRole('button', { name: 'Declarar no-show' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Declarar no-show' })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Declarar no-show' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('El no-show solo puede declararse')
  })
})
