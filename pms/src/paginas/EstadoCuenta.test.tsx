import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { Alojamiento } from '../api/alojamiento'
import type { Cargo, Folio } from '../api/folio'
import { alojamientoDePrueba } from '../pruebas/alojamientoDePrueba'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const RESERVA = '/api/reservas/RES-2026-00042'

const CARGOS: Cargo[] = [
  { id: 'CAR-1', tipo: 'HOSPEDAJE', concepto: 'Hospedaje', valor: 1250000, sentido: null, fecha: '2026-11-14' },
  { id: 'CAR-2', tipo: 'IMPUESTO', concepto: 'IVA del alojamiento (19 %)', valor: 237500, sentido: null, fecha: '2026-11-14' },
  { id: 'CAR-3', tipo: 'SERVICIO_ADICIONAL', concepto: 'Desayuno', valor: 50000, sentido: null, fecha: '2026-11-15', cantidad: 2 },
]

function folioDePrueba(cambios: Partial<Folio> = {}): Folio {
  return {
    id: 'FOL-1', reserva: 'RES-2026-00042', cargos: CARGOS,
    pagos: [{ id: 'PAG-1', medio: 'TRANSFERENCIA', monto: 1000000, tipo: 'ABONO', fecha: '2026-11-10' }],
    totalPagado: 1000000, saldo: 537500, situacionSaldo: 'PENDIENTE', cerrado: false, ...cambios,
  }
}

const POLITICA = {
  id: 'POL-3', version: 2,
  tramos: [
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
    { antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

const TITULAR = {
  id: 'TIT-7', nombre: 'Ana Ríos', tipoDocumento: 'CC', numeroDocumento: '52481', correo: 'ana@correo.co',
  telefono: '3001234567', validado: true, validadoEn: null, validadoPor: null,
}

function apiCon({ folio = folioDePrueba(), alojamiento = alojamientoDePrueba({ rnt: '98765', normas: 'Silencio desde las 10 p. m.' }), titular = [200, TITULAR] as [number, unknown], reserva = [200, reservaDePrueba({ estado: 'EN_CURSO' })] as [number, unknown] }: {
  folio?: Folio
  alojamiento?: Alojamiento
  titular?: [number, unknown]
  reserva?: [number, unknown]
} = {}) {
  return laApiRespondePorRuta({
    [RESERVA]: reserva,
    [`${RESERVA}/folio`]: [200, folio],
    '/api/alojamiento': [200, alojamiento],
    '/api/politicas/POL-3': [200, POLITICA],
    '/api/titulares/TIT-7': titular,
  })
}

function abrir(ruta = '/reservas/RES-2026-00042/estado-cuenta', rol: Rol = 'RECEPCIONISTA') {
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

async function hoja() {
  return screen.findByRole('article', { name: 'Estado de cuenta' })
}

describe('estado de cuenta (DEC-133)', () => {
  it('trae el alojamiento, el huésped, la estancia y la frase de que no es una factura electrónica', async () => {
    apiCon()
    abrir()
    const documento = await hoja()

    expect(within(documento).getByText('Puerta al Sol')).toBeInTheDocument()
    expect(within(documento).getByText('Vereda Palestina km 2 · Salento (Quindío)')).toBeInTheDocument()
    expect(within(documento).getByText('RNT 98765')).toBeInTheDocument()
    expect(within(documento).getByText('RES-2026-00042')).toBeInTheDocument()
    expect(within(documento).getByText('Este documento no es una factura electrónica.')).toBeInTheDocument()

    const huesped = within(documento).getByRole('region', { name: 'Huésped' })
    expect(within(huesped).getByText('Ana Ríos')).toBeInTheDocument()
    expect(within(huesped).getByText('C.C. 52481')).toBeInTheDocument()
    expect(within(huesped).getByText('ana@correo.co')).toBeInTheDocument()

    const estancia = within(documento).getByRole('region', { name: 'Estancia' })
    expect(within(estancia).getByText('APT-101')).toBeInTheDocument()
    expect(within(estancia).getByText('14 nov 2026, desde las 3:00 p. m.')).toBeInTheDocument()
    expect(within(estancia).getByText('17 nov 2026, hasta las 12:00 p. m.')).toBeInTheDocument()
    expect(within(estancia).getByText('En curso')).toBeInTheDocument()
  })

  it('cada cargo con el IVA aparte, los pagos, el total y el saldo', async () => {
    apiCon()
    abrir()
    const documento = await hoja()

    const cargos = within(documento).getByRole('table', { name: 'Cargos' })
    expect(within(cargos).getByText('IVA del alojamiento (19 %)')).toBeInTheDocument()
    expect(within(cargos).getByText('Desayuno × 2')).toBeInTheDocument()
    expect(within(cargos).getByRole('rowheader', { name: 'Subtotal sin IVA' }).closest('tr')).toHaveTextContent('$ 1.300.000')
    expect(within(cargos).getByRole('rowheader', { name: 'IVA' }).closest('tr')).toHaveTextContent('$ 237.500')
    expect(within(cargos).getByRole('rowheader', { name: 'Total' }).closest('tr')).toHaveTextContent('$ 1.537.500')

    const pagos = within(documento).getByRole('table', { name: 'Pagos' })
    expect(within(pagos).getByText('Abono · Transferencia')).toBeInTheDocument()
    expect(within(pagos).getByRole('rowheader', { name: 'Total pagado' }).closest('tr')).toHaveTextContent('$ 1.000.000')

    const resumen = within(documento).getByRole('region', { name: 'Resumen' })
    expect(within(resumen).getByText('Saldo pendiente').parentElement).toHaveTextContent('$ 537.500')
  })

  it('la política congelada en la reserva y las normas de la casa', async () => {
    apiCon()
    abrir()
    const documento = await hoja()

    const politica = within(documento).getByRole('region', { name: 'Política de cancelación' })
    expect(within(politica).getByText(/versión 2/)).toBeInTheDocument()
    expect(within(politica).getByText('48 horas o más antes de la entrada: no se retiene nada.')).toBeInTheDocument()
    expect(within(politica).getByText('Menos de 48 horas antes de la entrada: se retiene el 50 % del valor total.')).toBeInTheDocument()
    expect(within(politica).getByText('Si el huésped no se presenta: se retiene el 100 % del valor total.')).toBeInTheDocument()
    expect(within(documento).getByRole('region', { name: 'Normas de la casa' })).toHaveTextContent('Silencio desde las 10 p. m.')
  })

  it('sin IVA no muestra el subtotal ni la línea del IVA; sin normas, no hay sección', async () => {
    apiCon({ folio: folioDePrueba({ cargos: [CARGOS[0]], saldo: 0, totalPagado: 1250000, situacionSaldo: 'AL_DIA' }), alojamiento: alojamientoDePrueba() })
    abrir()
    const documento = await hoja()

    const cargos = within(documento).getByRole('table', { name: 'Cargos' })
    expect(within(cargos).queryByRole('rowheader', { name: 'IVA' })).not.toBeInTheDocument()
    expect(within(cargos).queryByRole('rowheader', { name: 'Subtotal sin IVA' })).not.toBeInTheDocument()
    expect(within(documento).getByText('Sin saldo pendiente')).toBeInTheDocument()
    expect(within(documento).queryByRole('region', { name: 'Normas de la casa' })).not.toBeInTheDocument()
  })

  it('una reserva exenta por pasaporte lo dice en los totales', async () => {
    const reserva = reservaDePrueba({ estado: 'EN_CURSO' })
    apiCon({
      folio: folioDePrueba({ cargos: [CARGOS[0]] }),
      reserva: [200, { ...reserva, valor: { ...reserva.valor, ivaPorcentaje: 19, ivaExento: true, iva: 0, totalConIva: 1250000 } }],
    })
    abrir()
    const cargos = within(await hoja()).getByRole('table', { name: 'Cargos' })

    expect(within(cargos).getByRole('rowheader', { name: 'IVA (19 %) · exento por pasaporte' }).closest('tr')).toHaveTextContent('$ 0')
    expect(within(cargos).queryByRole('rowheader', { name: 'Subtotal sin IVA' })).not.toBeInTheDocument()
  })

  it('el logo es decorativo y, si no carga, no deja el ícono roto', async () => {
    apiCon({ alojamiento: alojamientoDePrueba({ marca: { ...alojamientoDePrueba().marca!, logoUrl: 'https://fotos.test/logo.png' } }) })
    abrir()
    const logo = (await hoja()).querySelector('img') as HTMLImageElement

    expect(logo).toHaveAttribute('alt', '')
    fireEvent.error(logo)
    expect(logo).not.toBeVisible()
  })

  it('si el titular no carga, sigue con el nombre de la reserva y sin documento', async () => {
    apiCon({ titular: [500, { estado: 500, codigo: 'ERROR', mensaje: 'Falló', detalles: [] }] })
    abrir()
    const huesped = within(await hoja()).getByRole('region', { name: 'Huésped' })

    expect(within(huesped).getByText('Ana Ríos')).toBeInTheDocument()
    expect(within(huesped).queryByText('Documento')).not.toBeInTheDocument()
  })

  it('«Imprimir o guardar en PDF» abre la impresión del navegador', async () => {
    apiCon()
    const imprimir = vi.fn()
    vi.stubGlobal('print', imprimir)
    const usuario = abrir()
    await hoja()

    await usuario.click(screen.getByRole('button', { name: 'Imprimir o guardar en PDF' }))
    expect(imprimir).toHaveBeenCalledOnce()
    expect(screen.getByRole('link', { name: /Volver al folio/ })).toHaveAttribute('href', '/reservas/RES-2026-00042/folio')
  })

  it('una reserva que no existe dice «Reserva no encontrada»', async () => {
    apiCon({ reserva: [404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe', detalles: [] }] })
    abrir()
    expect(await screen.findByRole('heading', { level: 1, name: 'Reserva no encontrada' })).toBeInTheDocument()
  })

  it('si falla el folio ofrece «Reintentar» y al reintentar muestra el documento', async () => {
    let falla = true
    laApiRespondePorRuta({
      [RESERVA]: [200, reservaDePrueba({ estado: 'EN_CURSO' })],
      [`${RESERVA}/folio`]: () => (falla ? [500, { estado: 500, codigo: 'ERROR', mensaje: 'Falló el servidor', detalles: [] }] : [200, folioDePrueba()]),
      '/api/alojamiento': [200, alojamientoDePrueba()],
      '/api/politicas/POL-3': [200, POLITICA],
      '/api/titulares/TIT-7': [200, TITULAR],
    })
    const usuario = abrir()

    expect(await screen.findByText('Error · No pudimos preparar el estado de cuenta')).toBeInTheDocument()
    falla = false
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await hoja()).toBeInTheDocument()
  })

  it('el personal de servicio no lo puede abrir', async () => {
    apiCon()
    abrir(undefined, 'PERSONAL_SERVICIO')
    expect(await screen.findByRole('heading', { level: 1, name: /permiso/i })).toBeInTheDocument()
  })

  it('el folio lleva a «Estado de cuenta»', async () => {
    apiCon()
    abrir('/reservas/RES-2026-00042/folio')
    expect(await screen.findByRole('link', { name: 'Estado de cuenta' })).toHaveAttribute(
      'href', '/reservas/RES-2026-00042/estado-cuenta',
    )
  })
})
