import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { FichaApartamento } from '../api/apartamentos'
import type { Tarifa, Temporada } from '../api/tarifas'
import { alojamientoDePrueba } from '../pruebas/alojamientoDePrueba'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'
import type { Rol } from '../sesion/roles'

const base: Temporada = { id: 'TEM-1', nombre: 'Temporada base', fechaInicio: null, fechaFin: null, esBase: true,
  estanciaMinimaNoches: 0, activa: true }
const alta: Temporada = { id: 'TEM-2', nombre: 'Temporada alta fin de año', fechaInicio: '2026-12-15',
  fechaFin: '2027-01-15', esBase: false, estanciaMinimaNoches: 3, activa: true }
const santa: Temporada = { id: 'TEM-3', nombre: 'Semana Santa', fechaInicio: '2027-03-29', fechaFin: '2027-04-05',
  esBase: false, estanciaMinimaNoches: 2, activa: true }
const puente: Temporada = { id: 'TEM-4', nombre: 'Puente de noviembre', fechaInicio: '2026-11-14',
  fechaFin: '2026-11-17', esBase: false, estanciaMinimaNoches: 0, activa: false }
const CALENDARIO = [base, alta, santa, puente]

function apartamento(codigo: string, nombre: string, activo: boolean): FichaApartamento {
  return { codigo, nombre, descripcion: null, capacidad: 4, dormitorios: 2, estadoOperativo: 'PREPARADO', activo,
    imagenes: [], caracteristicas: ['Wifi'], bloqueos: [], limiteMascotas: null }
}
const APT101 = apartamento('APT-101', 'Penthouse Mirador', true)
const APT103 = apartamento('APT-103', 'Estudio Terraza', false)

function tarifa(apartamento: string, temporada: string, valor: number, version = 1, id = `${apartamento}-${temporada}-${version}`): Tarifa {
  return { id, apartamento, temporada, valorPorOcupante: valor, version, vigenteDesde: '2026-11-02' }
}

const tablaTarifas = async () => screen.findByRole('table', { name: 'Tarifas por apartamento y temporada' })
const tablaTemporadas = async () => screen.findByRole('table', { name: 'Temporadas activas' })

const error = (estado: number, codigo: string, mensaje: string): [number, unknown] =>
  [estado, { estado, codigo, mensaje, detalles: [] }]

function api(rutas: Parameters<typeof laApiRespondePorRuta>[0] = {}) {
  return laApiRespondePorRuta({
    '/api/alojamiento': [200, alojamientoDePrueba()],
    '/api/temporadas': [200, { temporadas: CALENDARIO }],
    '/api/apartamentos': [200, paginaDe([APT101, APT103])],
    '/api/apartamentos/APT-101/tarifas': [200, paginaDe([
      tarifa('APT-101', 'TEM-1', 120000), tarifa('APT-101', 'TEM-2', 180000, 2), tarifa('APT-101', 'TEM-3', 160000), tarifa('APT-101', 'TEM-4', 90000),
    ])],
    '/api/apartamentos/APT-103/tarifas': [200, paginaDe([tarifa('APT-103', 'TEM-1', 80000), tarifa('APT-103', 'TEM-2', 120000)])],
    ...rutas,
  })
}

function abrir(rol: Rol = 'ADMINISTRADOR') {
  entrarComo(rol)
  render(
    <MemoryRouter initialEntries={['/tarifas']}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

const envios = (fetch: ReturnType<typeof api>, metodo: string) => fetch.mock.calls
  .filter(([, opciones]) => (opciones as RequestInit | undefined)?.method === metodo)
  .map(([url, opciones]) => ({ url, cuerpo: (opciones as RequestInit).body ? JSON.parse(String((opciones as RequestInit).body)) : undefined }))

describe('temporadas y tarifas', () => {
  it('muestra la base primero, las inactivas aparte y cada tarifa con su valor o «Sin tarifa»', async () => {
    api()
    abrir()
    const temporadas = await screen.findByRole('table', { name: 'Temporadas activas' })
    const filas = within(temporadas).getAllByRole('row').slice(1)
    expect(filas[0]).toHaveTextContent('Temporada base')
    expect(filas[0]).toHaveTextContent('Cubre los días que no caen en otra temporada')
    expect(filas[0]).toHaveTextContent('Siempre activa')
    expect(within(filas[0]).queryByRole('button')).not.toBeInTheDocument()
    expect(filas[1]).toHaveTextContent('15 dic 2026 – 15 ene 2027')
    expect(filas[1]).toHaveTextContent('3 noches')
    expect(within(filas[1]).getByRole('button', { name: 'Desactivar Temporada alta fin de año' })).toBeInTheDocument()
    expect(screen.getByText('Ver inactivas (1)')).toBeInTheDocument()
    expect(within(screen.getByRole('table', { name: 'Temporadas inactivas' })).getByText('Sin mínimo')).toBeInTheDocument()

    const tarifas = screen.getByRole('table', { name: 'Tarifas por apartamento y temporada' })
    expect(within(tarifas).getAllByRole('columnheader').map((c) => c.textContent))
      .toEqual(['Apartamento', 'Temporada base', 'Temporada alta fin de año', 'Semana Santa', 'Historial'])
    const fila103 = within(tarifas).getByRole('row', { name: /APT-103/ })
    expect(within(fila103).getByRole('button', { name: 'Poner la tarifa de APT-103 en Semana Santa: sin tarifa' }))
      .toHaveTextContent('Sin tarifa')
    expect(within(fila103).getByText('No se puede poner a la venta')).toBeInTheDocument()
    const fila101 = within(tarifas).getByRole('row', { name: /APT-101/ })
    expect(within(fila101).getByRole('button', { name: /Temporada alta fin de año: \$ 180\.000/ })).toBeInTheDocument()
    expect(within(fila101).queryByText('No se puede poner a la venta')).not.toBeInTheDocument()
    expect(screen.queryByText(/TEM-\d/)).not.toBeInTheDocument()
  })

  it('con solo la temporada base lo dice; si la API falla, ofrece «Reintentar»', async () => {
    api({ '/api/temporadas': [200, { temporadas: [base] }] })
    abrir()
    expect(await screen.findByText('No hay otras temporadas: todas las noches se cobran con la temporada base.'))
      .toBeInTheDocument()
  })

  it('si la API falla, explica y ofrece «Reintentar»', async () => {
    api({ '/api/temporadas': error(500, 'ERROR_INTERNO', 'Error interno') })
    abrir()
    expect(await screen.findByText('No pudimos cargar las temporadas y tarifas', { exact: false })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('recepción no puede entrar', async () => {
    api()
    abrir('RECEPCIONISTA')
    expect(await screen.findByRole('heading', { name: 'No tienes permiso para ver esta pantalla' })).toBeInTheDocument()
  })
})

describe('cambiar una tarifa', () => {
  it('muestra la vigente, pide más que cero y guarda la versión nueva en la tabla', async () => {
    const fetch = api({
      '/api/apartamentos/APT-101/tarifas/TEM-2': [200, tarifa('APT-101', 'TEM-2', 200000, 3)],
    })
    const usuario = abrir()
    await usuario.click(within(await tablaTarifas()).getByRole('button', { name: /Cambiar la tarifa de APT-101 en Temporada alta fin de año/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'APT-101 · Temporada alta fin de año' })
    expect(within(dialogo).getByText('Hoy: $ 180.000, desde el 2 nov 2026.')).toBeInTheDocument()

    const campo = within(dialogo).getByLabelText('Valor por persona y noche')
    await usuario.type(campo, '0')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar tarifa' }))
    expect(within(dialogo).getByText('Escribe un valor mayor que cero.')).toBeInTheDocument()
    expect(envios(fetch, 'PUT')).toHaveLength(0)

    await usuario.clear(campo)
    await usuario.type(campo, '200000')
    expect(campo).toHaveValue('200.000')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar tarifa' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(envios(fetch, 'PUT')).toEqual([{ url: '/api/apartamentos/APT-101/tarifas/TEM-2', cuerpo: { valorPorOcupante: 200000 } }])
    expect(within(await tablaTarifas()).getByRole('button', { name: /Cambiar la tarifa de APT-101 en Temporada alta fin de año: \$ 200\.000/ }))
      .toBeInTheDocument()
    expect(screen.getByText('Tarifa guardada')).toBeInTheDocument()
  })

  it('el historial nombra las temporadas y marca las vigentes', async () => {
    api({
      '/api/apartamentos/APT-101/tarifas/historial': [200, paginaDe([
        tarifa('APT-101', 'TEM-2', 180000, 2), tarifa('APT-101', 'TEM-2', 170000, 1), tarifa('APT-101', 'TEM-4', 90000),
      ])],
    })
    const usuario = abrir()
    await usuario.click(within(await tablaTarifas()).getByRole('button', { name: 'Historial de APT-101' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Historial de tarifas · APT-101' })
    const filas = within(await within(dialogo).findByRole('table')).getAllByRole('row').slice(1)
    expect(filas[0]).toHaveTextContent('Temporada alta fin de año Vigente$ 180.0002')
    expect(filas[1]).not.toHaveTextContent('Vigente')
    // Puente de noviembre está inactiva: su tarifa ya no se cobra, aunque la API la traiga entre las vigentes
    expect(filas[2]).toHaveTextContent('Puente de noviembre')
    expect(filas[2]).not.toHaveTextContent('Vigente')
  })
})

describe('nueva temporada', () => {
  async function abrirNueva(fetchRutas: Parameters<typeof api>[0] = {}) {
    const fetch = api(fetchRutas)
    const usuario = abrir()
    await usuario.click(await screen.findByRole('button', { name: /Nueva temporada/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Nueva temporada' })
    await within(dialogo).findByLabelText(/APT-101/)
    return { fetch, usuario, dialogo }
  }

  it('exige la tarifa de los apartamentos a la venta y avisa del solapamiento con el nombre de la otra (TAR-03 · TEM-02)', async () => {
    const { fetch, usuario, dialogo } = await abrirNueva()
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Reyes')
    await usuario.type(within(dialogo).getByLabelText('Desde'), '2027-01-15')
    await usuario.type(within(dialogo).getByLabelText('Hasta'), '2027-01-20')
    await usuario.type(within(dialogo).getByLabelText('Estancia mínima'), '0')
    expect(within(dialogo).getByLabelText(/APT-103/)).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear temporada' }))

    expect(within(dialogo).getByText(
      'La temporada Reyes se solapa con Temporada alta fin de año (15 dic 2026 – 15 ene 2027). No pueden compartir ni un día.',
    )).toBeInTheDocument()
    expect(within(dialogo).getByText('Está a la venta: necesita su tarifa en esta temporada.')).toBeInTheDocument()
    expect(envios(fetch, 'POST')).toHaveLength(0)
  })

  it('crea con las tarifas escritas; un rechazo de la API sale con los nombres, no con los ids', async () => {
    let intentos = 0
    const fetch = await abrirNueva({
      '/api/temporadas': (metodo) => {
        if (metodo !== 'POST') return [200, { temporadas: CALENDARIO }]
        intentos += 1
        return intentos === 1
          ? error(409, 'REGLA_NEGOCIO', 'La temporada TEM-9 se solapa con TEM-3')
          : [201, { temporada: { ...santa, id: 'TEM-9', nombre: 'Reyes' }, tarifas: [] }]
      },
    })
    const { usuario, dialogo } = fetch
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Reyes')
    await usuario.type(within(dialogo).getByLabelText('Desde'), '2027-01-16')
    await usuario.type(within(dialogo).getByLabelText('Hasta'), '2027-01-20')
    await usuario.type(within(dialogo).getByLabelText('Estancia mínima'), '2')
    await usuario.type(within(dialogo).getByLabelText(/APT-101/), '150000')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear temporada' }))

    expect(await within(dialogo).findByText('La temporada Reyes se solapa con Semana Santa')).toBeInTheDocument()

    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear temporada' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(envios(fetch.fetch, 'POST')[1]).toEqual({ url: '/api/temporadas', cuerpo: {
      nombre: 'Reyes', fechaInicio: '2027-01-16', fechaFin: '2027-01-20', estanciaMinimaNoches: 2,
      tarifas: [{ apartamento: 'APT-101', valorPorOcupante: 150000 }],
    } })
    expect(await screen.findByText('Temporada creada')).toBeInTheDocument()
  })
})

describe('cambiar fechas y desactivar', () => {
  it('no deja cambiar las fechas a unas que se cruzan con otra temporada', async () => {
    const fetch = api()
    const usuario = abrir()
    await usuario.click(within(await tablaTemporadas()).getByRole('button', { name: 'Cambiar fechas de Semana Santa' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Cambiar fechas' })
    const desde = within(dialogo).getByLabelText('Desde')
    await usuario.clear(desde)
    await usuario.type(desde, '2027-01-10')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar fechas' }))
    expect(within(dialogo).getByText(/se solapa con Temporada alta fin de año/)).toBeInTheDocument()
    expect(envios(fetch, 'PUT')).toHaveLength(0)
  })

  it('desactiva tras confirmar y la temporada pasa a las inactivas', async () => {
    const fetch = api({
      '/api/temporadas/TEM-2/desactivar': [200, { temporadas: [base, { ...alta, activa: false }, santa, puente] }],
    })
    const usuario = abrir()
    await usuario.click(within(await tablaTemporadas()).getByRole('button', { name: 'Desactivar Temporada alta fin de año' }))
    const dialogo = await screen.findByRole('dialog', { name: '¿Desactivar Temporada alta fin de año?' })
    expect(within(dialogo).getByText('No se puede volver a activar.')).toBeInTheDocument()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Desactivar temporada' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(envios(fetch, 'PUT').map((e) => e.url)).toEqual(['/api/temporadas/TEM-2/desactivar'])
    expect(screen.getByText('Ver inactivas (2)')).toBeInTheDocument()
    expect(within(screen.getByRole('table', { name: 'Tarifas por apartamento y temporada' }))
      .queryByRole('columnheader', { name: 'Temporada alta fin de año' })).not.toBeInTheDocument()
  })
})
