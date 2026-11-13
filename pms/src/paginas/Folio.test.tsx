import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Cargo, Folio, Pago } from '../api/folio'
import type { Reserva } from '../api/reservas'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const RUTA = '/api/reservas/RES-2026-00042'

const CARGOS: Cargo[] = [
  { id: 'CAR-1', tipo: 'HOSPEDAJE', concepto: 'Hospedaje', valor: 1350000, sentido: null, fecha: '2026-11-09' },
  { id: 'CAR-2', tipo: 'SERVICIO_ADICIONAL', concepto: 'Parqueadero', valor: 60000, sentido: null, fecha: '2026-11-09' },
  { id: 'CAR-4', tipo: 'SERVICIO_ADICIONAL', concepto: 'Agua', valor: 10000, sentido: null, fecha: '2026-11-11', cantidad: 2 },
  { id: 'CAR-5', tipo: 'SERVICIO_ADICIONAL', concepto: 'Lavandería', valor: 45000, sentido: null, fecha: '2026-11-11' },
  {
    id: 'CAR-6', tipo: 'AJUSTE', concepto: 'Corrección de CAR-5: se cargó a la habitación equivocada', valor: 45000,
    sentido: 'DISMINUYE', fecha: '2026-11-11', corrigeA: 'CAR-5', autorNombre: 'Andrea Ruiz',
  },
  { id: 'CAR-7', tipo: 'SERVICIO_ADICIONAL', concepto: 'Minibar', valor: 38000, sentido: null, fecha: '2026-11-12' },
]

const PAGOS: Pago[] = [
  { id: 'PAG-1', medio: 'TRANSFERENCIA', monto: 600000, tipo: 'ABONO', fecha: '2026-11-10' },
  { id: 'PAG-2', medio: 'EFECTIVO', monto: 200000, tipo: 'ABONO', fecha: '2026-11-11' },
  {
    id: 'PAG-3', medio: 'EFECTIVO', monto: 200000, tipo: 'REVERSO', fecha: '2026-11-11', reversaDe: 'PAG-2',
    autorNombre: 'Andrea Ruiz',
  },
  { id: 'PAG-4', medio: 'TARJETA', monto: 500000, tipo: 'ABONO', fecha: '2026-11-12' },
]

function folioDePrueba(cambios: Partial<Folio> = {}): Folio {
  return {
    id: 'FOL-1', reserva: 'RES-2026-00042', cargos: CARGOS, pagos: PAGOS, totalPagado: 1100000, saldo: 403000,
    situacionSaldo: 'PENDIENTE', cerrado: false, autorizacion: null, ...cambios,
  }
}

const ALOJAMIENTO = {
  nombre: 'Puerta al Sol',
  parametros: { anticipoPct: 30, horaLimiteNoShow: '22:00:00' },
  mediosPago: ['EFECTIVO', 'TRANSFERENCIA', 'PAGO_EN_LINEA'],
  serviciosAdicionales: [
    { id: 'SRV-1', nombre: 'Agua', generaCargo: true, valor: 5000, activo: true },
    { id: 'SRV-2', nombre: 'Minibar', generaCargo: true, valor: 38000, activo: true },
    { id: 'SRV-3', nombre: 'Lavandería', generaCargo: true, valor: 45000, activo: true },
    { id: 'SRV-4', nombre: 'Wifi', generaCargo: false, valor: 0, activo: true },
    { id: 'SRV-5', nombre: 'Spa', generaCargo: true, valor: 90000, activo: false },
  ],
}

const RECHAZO = (motivo: string, mensaje = 'Rechazado'): [number, unknown] => [
  409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje, detalles: [], motivo },
]

/** API simulada que recuerda lo que cambia, como la de verdad. */
function apiCon({ reserva = {}, folio = folioDePrueba(), rechazar = {} }: {
  reserva?: Partial<Reserva>
  folio?: Folio
  rechazar?: Record<string, [number, unknown]>
} = {}) {
  const estado = { reserva: reservaDePrueba({ estado: 'EN_CURSO', entrada: '2026-11-10', salida: '2026-11-13', ...reserva }), folio }
  const accion = (nombre: string, hacer: () => unknown) => (): [number, unknown] =>
    rechazar[nombre] ?? [200, hacer()]
  const conFolio = (cambios: Partial<Folio>) => () => {
    estado.folio = { ...estado.folio, ...cambios }
    return estado.folio
  }
  return laApiRespondePorRuta({
    [RUTA]: () => [200, estado.reserva],
    [`${RUTA}/folio`]: () => [200, estado.folio],
    '/api/alojamiento': [200, ALOJAMIENTO],
    [`${RUTA}/folio/servicios`]: accion('servicios', conFolio({ saldo: 418000 })),
    [`${RUTA}/folio/cargos/CAR-7/revertir`]: accion('revertirCargo', conFolio({ saldo: 365000 })),
    [`${RUTA}/folio/pagos/PAG-1/revertir`]: accion('revertirPago', conFolio({ totalPagado: 500000, saldo: 1003000 })),
    [`${RUTA}/folio/pagos`]: accion('pagos', conFolio({ totalPagado: 1503000, saldo: 0, situacionSaldo: 'AL_DIA' })),
    [`${RUTA}/folio/devoluciones`]: accion('devoluciones', conFolio({ saldo: 0, situacionSaldo: 'AL_DIA' })),
    [`${RUTA}/folio/cerrar`]: accion('cerrar', conFolio({ cerrado: true })),
    [`${RUTA}/registrar-salida`]: accion('salida', () => {
      estado.reserva = { ...estado.reserva, estado: 'FINALIZADA' }
      estado.folio = { ...estado.folio, cerrado: true }
      return {
        codigo: 'RES-2026-00042', estado: 'FINALIZADA', apartamento: 'APT-101', estadoOperativo: 'PENDIENTE_PREPARACION',
        folioCerrado: true, saldo: estado.folio.saldo, situacionSaldo: estado.folio.situacionSaldo,
      }
    }),
  })
}

function llamadasA(fetch: ReturnType<typeof apiCon>, sufijo: string) {
  return fetch.mock.calls
    .filter(([url]) => String(url).endsWith(sufijo))
    .map(([, opciones]) => ({
      metodo: opciones?.method ?? 'GET',
      cuerpo: opciones?.body ? JSON.parse(String(opciones.body)) : undefined,
    }))
}

function abrir(rol: Rol = 'RECEPCIONISTA') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={['/reservas/RES-2026-00042/folio']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

async function abrirDialogo(usuario: ReturnType<typeof userEvent.setup>, boton: string, nombre = boton) {
  await usuario.click(await screen.findByRole('button', { name: boton }))
  return screen.findByRole('dialog', { name: nombre })
}

describe('Folio (RN-15 · RN-16)', () => {
  it('muestra el saldo, los cargos y los pagos con cada reverso debajo de lo que revierte', async () => {
    apiCon()
    abrir()

    const resumen = await screen.findByRole('region', { name: 'Resumen del folio' })
    expect(within(resumen).getByText('$ 403.000')).toBeInTheDocument()
    expect(within(resumen).getByText('Saldo pendiente')).toBeInTheDocument()
    expect(within(resumen).getByText('$ 1.458.000')).toBeInTheDocument()

    const cargos = screen.getByRole('table', { name: 'Cargos' })
    const filas = within(cargos).getAllByRole('row').slice(1).map((fila) => fila.textContent)
    expect(filas[2]).toContain('Agua × 2')
    expect(filas[3]).toMatch(/Lavandería.*Revertido/)
    expect(filas[4]).toContain('Corrección de Lavandería: se cargó a la habitación equivocada por Andrea Ruiz')
    expect(filas[4]).toContain('− $ 45.000')

    const pagos = screen.getByRole('table', { name: 'Pagos' })
    expect(within(pagos).getByText('Reverso del abono')).toBeInTheDocument()
    expect(within(pagos).getByText('por Andrea Ruiz')).toBeInTheDocument()
  })

  it('solo los consumos se corrigen; lo revertido y las correcciones no tienen acciones', async () => {
    apiCon()
    abrir()

    await screen.findByRole('table', { name: 'Cargos' })
    expect(screen.getByRole('button', { name: 'Corregir Agua × 2 del 11 nov 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Corregir Minibar del 12 nov 2026' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Corregir Hospedaje/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Corregir Parqueadero/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Revertir Hospedaje del 9 nov 2026' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Lavandería/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Revertir Tarjeta del 12 nov 2026' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Revertir Efectivo/ })).not.toBeInTheDocument()
  })

  it('reserva confirmada: sin cargar consumos ni registrar la salida', async () => {
    apiCon({ reserva: { estado: 'CONFIRMADA' } })
    abrir()

    expect(await screen.findByRole('button', { name: 'Registrar pago' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cargar consumo' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar salida' })).not.toBeInTheDocument()
  })

  it('folio cerrado con autorización: sin acciones y con quién lo autorizó', async () => {
    apiCon({
      reserva: { estado: 'FINALIZADA' },
      folio: folioDePrueba({
        cerrado: true,
        autorizacion: { motivo: 'Paga la empresa', fechaHora: '2026-11-13T11:42:00', autorNombre: 'Andrés Morales' },
      }),
    })
    abrir()

    expect(await screen.findByText('Folio cerrado')).toBeInTheDocument()
    expect(screen.getByText('Andrés Morales')).toBeInTheDocument()
    expect(screen.getByText('Paga la empresa')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Revertir|Registrar|Cargar|Corregir|Cerrar folio/ })).not.toBeInTheDocument()
  })

  it('una reserva que no existe dice que no se encontró', async () => {
    laApiRespondePorRuta({ '/api/alojamiento': [200, ALOJAMIENTO] })
    abrir()

    expect(await screen.findByRole('heading', { name: 'Reserva no encontrada' })).toBeInTheDocument()
  })
})

describe('Cargar y corregir consumos (CAR-05 · BL-12 · DEC-107)', () => {
  it('carga un consumo con su cantidad y muestra el total del catálogo', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Cargar consumo')

    // Solo los activos y con cobro
    expect(within(dialogo).getAllByRole('option').map((opcion) => opcion.textContent)).toEqual([
      'Agua · $ 5.000', 'Minibar · $ 38.000', 'Lavandería · $ 45.000',
    ])
    const cantidad = within(dialogo).getByLabelText('Cantidad')
    await usuario.clear(cantidad)
    await usuario.type(cantidad, '3')
    expect(within(dialogo).getByText('$ 15.000')).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cargar consumo' }))

    expect(await screen.findByText('Consumo cargado')).toBeInTheDocument()
    expect(llamadasA(fetch, '/folio/servicios')).toEqual([{ metodo: 'POST', cuerpo: { servicioId: 'SRV-1', cantidad: 3 } }])
  })

  it('una cantidad de 0 se marca junto al campo y no se envía', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Cargar consumo')

    await usuario.clear(within(dialogo).getByLabelText('Cantidad'))
    await usuario.type(within(dialogo).getByLabelText('Cantidad'), '0')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cargar consumo' }))

    expect(within(dialogo).getByLabelText('Cantidad')).toHaveAccessibleDescription('Escribe una cantidad de 1 a 99.')
    expect(llamadasA(fetch, '/folio/servicios')).toEqual([])
  })

  it('corregir revierte el consumo y propone cargarlo de nuevo; salir avisa que ya quedó revertido', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Corregir Minibar del 12 nov 2026' }))
    const paso1 = await screen.findByRole('dialog', { name: 'Corregir consumo' })

    await usuario.type(within(paso1).getByLabelText(/Motivo/), 'Eran dos')
    await usuario.click(within(paso1).getByRole('button', { name: 'Revertir y continuar' }))

    const paso2 = await screen.findByRole('dialog', { name: 'Corregir consumo · cargar el correcto' })
    expect(within(paso2).getByText('Ya se revirtió Minibar')).toBeInTheDocument()
    expect(within(paso2).getByLabelText('Consumo')).toHaveValue('SRV-2')
    expect(llamadasA(fetch, '/cargos/CAR-7/revertir')).toEqual([{ metodo: 'POST', cuerpo: { motivo: 'Eran dos' } }])

    await usuario.click(within(paso2).getByRole('button', { name: 'Salir' }))
    expect(within(paso2).getByText(/El cargo ya quedó revertido/)).toBeInTheDocument()
    await usuario.click(within(paso2).getByRole('button', { name: 'Salir sin cargar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(llamadasA(fetch, '/folio/servicios')).toEqual([])
  })
})

describe('Revertir (RN-16 · CAR-03 · PAG-03)', () => {
  it('un cargo no se revierte sin motivo', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Revertir Minibar del 12 nov 2026' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Revertir cargo' })

    await usuario.click(within(dialogo).getByRole('button', { name: 'Revertir cargo' }))
    expect(within(dialogo).getByLabelText(/Motivo/)).toHaveAccessibleDescription('Escribe el motivo. 0 / 170')
    expect(llamadasA(fetch, '/revertir')).toEqual([])

    await usuario.type(within(dialogo).getByLabelText(/Motivo/), 'No lo consumió')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Revertir cargo' }))
    expect(await screen.findByText('Cargo revertido')).toBeInTheDocument()
    expect(llamadasA(fetch, '/cargos/CAR-7/revertir')).toEqual([{ metodo: 'POST', cuerpo: { motivo: 'No lo consumió' } }])
  })

  it('el hospedaje avisa que no se vuelve a cargar desde el folio', async () => {
    apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Revertir Hospedaje del 9 nov 2026' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Revertir cargo' })

    expect(within(dialogo).getByText(/Desde el folio no se puede volver a cargar/)).toBeInTheDocument()
  })

  it('si otro turno ya lo revirtió, lo dice en palabras de recepción', async () => {
    apiCon({ rechazar: { revertirCargo: RECHAZO('YA_REVERTIDO', 'El cargo CAR-7 ya fue corregido') } })
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Revertir Minibar del 12 nov 2026' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Revertir cargo' })

    await usuario.type(within(dialogo).getByLabelText(/Motivo/), 'Error')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Revertir cargo' }))
    expect(await within(dialogo).findByText(/Este movimiento ya fue revertido/)).toBeInTheDocument()
  })

  it('un pago se revierte sin motivo, con confirmación', async () => {
    const fetch = apiCon()
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: 'Revertir Transferencia del 10 nov 2026' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Revertir pago' })

    expect(within(dialogo).queryByRole('textbox')).not.toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Revertir pago' }))
    expect(await screen.findByText('Pago revertido')).toBeInTheDocument()
    expect(llamadasA(fetch, '/pagos/PAG-1/revertir')).toEqual([{ metodo: 'POST', cuerpo: undefined }])
  })
})

describe('Devolución y cierre (PAG-06 · CU-28 · RN-17)', () => {
  it('con saldo a favor propone devolverlo', async () => {
    const fetch = apiCon({ folio: folioDePrueba({ saldo: 62000, situacionSaldo: 'A_FAVOR' }) })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Registrar devolución')

    expect(within(dialogo).getByLabelText('Monto (COP)')).toHaveValue('62.000')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar devolución' }))
    expect(await screen.findByText('Devolución registrada')).toBeInTheDocument()
    expect(llamadasA(fetch, '/folio/devoluciones')[0].cuerpo).toMatchObject({ medio: 'EFECTIVO', monto: 62000 })
  })

  it('recepción no cierra un folio con saldo', async () => {
    apiCon({ reserva: { estado: 'CANCELADA' } })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Cerrar folio')

    expect(within(dialogo).getByText(/No puedes cerrar un folio con saldo/)).toBeInTheDocument()
    expect(within(dialogo).queryByRole('button', { name: /Cerrar folio|Autorizar y cerrar/ })).not.toBeInTheDocument()
  })

  it('el administrador lo cierra con el motivo de su autorización', async () => {
    const fetch = apiCon({ reserva: { estado: 'NO_SHOW' } })
    const usuario = abrir('ADMINISTRADOR')
    const dialogo = await abrirDialogo(usuario, 'Cerrar folio')

    await usuario.click(within(dialogo).getByRole('button', { name: 'Autorizar y cerrar' }))
    expect(llamadasA(fetch, '/folio/cerrar')).toEqual([])
    await usuario.type(within(dialogo).getByLabelText(/Motivo de la autorización/), 'Se cobra a la agencia')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Autorizar y cerrar' }))

    expect(await screen.findByText('Folio cerrado', { selector: 'strong' })).toBeInTheDocument()
    expect(llamadasA(fetch, '/folio/cerrar')).toEqual([
      { metodo: 'PUT', cuerpo: { motivoAutorizacion: 'Se cobra a la agencia' } },
    ])
  })

  it('con saldo en cero se cierra sin motivo', async () => {
    const fetch = apiCon({ reserva: { estado: 'CANCELADA' }, folio: folioDePrueba({ saldo: 0, situacionSaldo: 'AL_DIA' }) })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Cerrar folio')

    await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar folio' }))
    await screen.findByText('Ya no admite movimientos.')
    expect(llamadasA(fetch, '/folio/cerrar')).toEqual([{ metodo: 'PUT', cuerpo: undefined }])
  })
})

describe('Registrar salida (CU-22 · CU-38 · SAL-03 · DEC-96)', () => {
  const COMPLETOS = reservaDePrueba().ocupantes.map((ocupante) => ({ ...ocupante, datosTraFaltantes: [] }))

  it('sin saldo y con el TRA completo, registra la salida y dice cómo quedó', async () => {
    const fetch = apiCon({ reserva: { ocupantes: COMPLETOS }, folio: folioDePrueba({ saldo: 0, situacionSaldo: 'AL_DIA' }) })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Registrar salida')

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar salida' }))
    const resultado = await screen.findByRole('dialog', { name: 'Salida registrada' })
    expect(within(resultado).getByText(/Pendiente de preparación/)).toBeInTheDocument()
    expect(llamadasA(fetch, '/registrar-salida')).toEqual([{ metodo: 'PUT', cuerpo: undefined }])
  })

  it('con datos del TRA pendientes queda bloqueada y dice qué falta de quién', async () => {
    const ocupantes = COMPLETOS.map((ocupante, posicion) =>
      posicion === 0 ? { ...ocupante, datosTraFaltantes: ['GENERO', 'MOTIVO_VIAJE'] } : ocupante)
    apiCon({ reserva: { ocupantes }, folio: folioDePrueba({ saldo: 0, situacionSaldo: 'AL_DIA' }) })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Registrar salida')

    expect(within(dialogo).getByText('Faltan datos del TRA', { exact: false })).toBeInTheDocument()
    expect(within(dialogo).getByRole('listitem')).toHaveTextContent('Ana Ríos: género, motivo del viaje')
    const boton = within(dialogo).getByRole('button', { name: 'Registrar salida' })
    expect(boton).toBeDisabled()
    expect(boton).toHaveAccessibleDescription(/Faltan datos del TRA/)

    // Desde el aviso se abren los datos que faltan; al cerrarlos se vuelve a la salida
    await usuario.click(within(dialogo).getByRole('button', { name: 'Completar datos de Ana Ríos' }))
    const tra = await screen.findByRole('dialog', { name: 'Completar datos' })
    await usuario.click(within(tra).getByRole('button', { name: 'Cerrar' }))
    expect(await screen.findByRole('dialog', { name: 'Registrar salida' })).toBeInTheDocument()
  })

  it('el administrador no ve el motivo de autorización mientras falten datos del TRA', async () => {
    const ocupantes = COMPLETOS.map((ocupante) => ({ ...ocupante, datosTraFaltantes: ['GENERO'] }))
    apiCon({ reserva: { ocupantes } })
    const usuario = abrir('ADMINISTRADOR')
    const dialogo = await abrirDialogo(usuario, 'Registrar salida')

    expect(within(dialogo).queryByLabelText(/Motivo de la autorización/)).not.toBeInTheDocument()
    expect(within(dialogo).getByRole('button', { name: 'Registrar salida' })).toBeDisabled()
  })

  it('recepción con saldo pendiente no sale, pero puede cobrarlo ahí mismo', async () => {
    const fetch = apiCon({ reserva: { ocupantes: COMPLETOS } })
    const usuario = abrir()
    const dialogo = await abrirDialogo(usuario, 'Registrar salida')

    expect(within(dialogo).getByRole('button', { name: 'Registrar salida' })).toBeDisabled()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar pago' }))
    const pago = await screen.findByRole('dialog', { name: 'Registrar pago' })
    expect(within(pago).getByLabelText('Monto (COP)')).toHaveValue('403.000')
    await usuario.click(within(pago).getByRole('button', { name: 'Registrar pago' }))

    const deVuelta = await screen.findByRole('dialog', { name: 'Registrar salida' })
    await waitFor(() => expect(within(deVuelta).getByRole('button', { name: 'Registrar salida' })).toBeEnabled())
    expect(llamadasA(fetch, '/folio/pagos')).toHaveLength(1)
  })

  it('el administrador sale con saldo dejando el motivo de su autorización', async () => {
    const fetch = apiCon({ reserva: { ocupantes: COMPLETOS } })
    const usuario = abrir('ADMINISTRADOR')
    const dialogo = await abrirDialogo(usuario, 'Registrar salida')

    await usuario.type(within(dialogo).getByLabelText(/Motivo de la autorización/), 'Paga la empresa')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Autorizar y registrar salida' }))

    await screen.findByRole('dialog', { name: 'Salida registrada' })
    expect(llamadasA(fetch, '/registrar-salida')).toEqual([
      { metodo: 'PUT', cuerpo: { motivoAutorizacion: 'Paga la empresa' } },
    ])
  })
})
