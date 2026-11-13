import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Politica } from '../api/politicas'
import { alojamientoDePrueba } from '../pruebas/alojamientoDePrueba'
import { paginaDe } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta, llamada } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const V3: Politica = {
  id: 'POL-3',
  version: 3,
  tramos: [
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
    { antelacionMinHoras: 24, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
    { antelacionMinHoras: 0, penalizacion: { base: 'ANTICIPO_EXIGIDO', porcentaje: 100, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'PAGADO', porcentaje: 100, montoFijo: null },
}
const V2: Politica = { ...V3, id: 'POL-2', version: 2, tramos: V3.tramos.slice(1) }

const NO_HAY = [404, { estado: 404, codigo: 'NO_ENCONTRADO', mensaje: 'No existe', detalles: [] }] as [number, unknown]

/** API simulada que publica: la versión nueva pasa a ser la vigente. */
function apiCon({ versiones = [V3, V2], rechazo }: { versiones?: Politica[]; rechazo?: [number, unknown] } = {}) {
  let lista = versiones
  return laApiRespondePorRuta({
    '/api/alojamiento': [200, alojamientoDePrueba()],
    '/api/politicas/vigente': () => (lista[0] ? [200, lista[0]] : NO_HAY),
    '/api/politicas': (metodo) => {
      if (metodo === 'GET') return [200, paginaDe(lista)]
      if (rechazo) return rechazo
      const version = (lista[0]?.version ?? 0) + 1
      const nueva = { ...V3, id: `POL-${version}`, version }
      lista = [nueva, ...lista]
      return [201, nueva]
    },
  })
}

function abrir(direccion = '/politica') {
  entrarComo('ADMINISTRADOR')
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </MemoryRouter>,
  )
  return userEvent.setup()
}

describe('política de cancelación (CU-35 · POL-04)', () => {
  it('cuenta la vigente como «Ver política» y pliega las anteriores', async () => {
    apiCon()
    const usuario = abrir()

    const vigente = await screen.findByRole('article', { name: /Versión 3/ })
    expect(within(vigente).getByText('Vigente')).toBeInTheDocument()
    expect(within(vigente).getByText('48 horas o más antes de la entrada')).toBeInTheDocument()
    expect(within(vigente).getByText('Entre 24 y 48 horas antes de la entrada')).toBeInTheDocument()
    expect(within(vigente).getByText('Se retiene el 100 % del anticipo exigido')).toBeInTheDocument()
    expect(within(vigente).getByText('Se retiene el 100 % de lo pagado')).toBeInTheDocument()

    const anteriores = screen.getByRole('region', { name: 'Versiones anteriores' })
    expect(within(anteriores).queryByText('Versión 3')).not.toBeInTheDocument()
    await usuario.click(within(anteriores).getByText('Versión 2'))
    expect(within(anteriores).getByText('24 horas o más antes de la entrada')).toBeVisible()
  })

  it('sin política publicada ofrece publicar la primera', async () => {
    apiCon({ versiones: [] })
    abrir()
    expect(await screen.findByText('Todavía no hay política de cancelación')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Publicar la primera versión' })).toHaveAttribute('href', '/politica/publicar')
  })

  it('publicar parte de la vigente, revisa al escribir y vuelve con la versión nueva', async () => {
    const fetch = apiCon()
    const usuario = abrir('/politica/publicar')

    const horas = await screen.findByRole('textbox', { name: 'Horas de antelación, tramo 2' })
    expect(screen.getByRole('complementary', { name: 'Vista previa' })).toHaveTextContent('Entre 24 y 48 horas antes de la entrada')

    // Dos tramos en 48 horas: se marcan los dos, se oculta la vista previa y no deja publicar
    await usuario.clear(horas)
    await usuario.type(horas, '48')
    expect(screen.getAllByText('Otro tramo también empieza en 48 horas. Cambia las horas de uno de los dos.')).toHaveLength(2)
    expect(screen.getByText('La vista previa vuelve cuando se corrija lo marcado.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publicar nueva versión' })).toBeDisabled()

    await usuario.clear(horas)
    await usuario.type(horas, '12')
    await usuario.selectOptions(screen.getByRole('combobox', { name: 'Porcentaje o monto fijo, tramo 2' }), 'monto')
    const valor = screen.getByRole('textbox', { name: 'Monto en pesos que se retiene, tramo 2' })
    await usuario.clear(valor)
    await usuario.type(valor, '50.000')
    expect(screen.getByRole('complementary', { name: 'Vista previa' })).toHaveTextContent('Se retienen $ 50.000, sin pasar del valor total')

    await usuario.click(screen.getByRole('button', { name: 'Publicar nueva versión' }))
    expect(await screen.findByText('Versión 4 publicada')).toBeInTheDocument()
    const envio = fetch.mock.calls.map((_, n) => llamada(fetch, n)).find((l) => l.metodo === 'POST')
    expect(envio?.cuerpo.tramos[1]).toEqual({ antelacionMinHoras: 12, penalizacion: { base: 'VALOR_TOTAL', montoFijo: 50000 } })
  })

  it('quitar tramos por debajo del mínimo lo dice debajo de la lista', async () => {
    apiCon()
    const usuario = abrir('/politica/publicar')
    await usuario.click(await screen.findByRole('button', { name: 'Quitar tramo 1' }))
    await usuario.click(screen.getByRole('button', { name: 'Quitar tramo 1' }))
    expect(screen.getByText('Hacen falta al menos 2 tramos. Agrega otro tramo.')).toBeInTheDocument()
  })

  it('el rechazo del servidor sale arriba del formulario', async () => {
    apiCon({ rechazo: [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'La política debe tener al menos 3 tramos', detalles: [] }] })
    const usuario = abrir('/politica/publicar')
    await usuario.click(await screen.findByRole('button', { name: 'Publicar nueva versión' }))
    expect(await screen.findByText('Error · No se publicó la versión nueva')).toBeInTheDocument()
    expect(screen.getByText('La política debe tener al menos 3 tramos')).toBeInTheDocument()
  })

  it('pregunta antes de salir con cambios sin publicar', async () => {
    apiCon()
    const usuario = abrir('/politica/publicar')
    await usuario.click(await screen.findByRole('button', { name: 'Agregar tramo' }))
    await usuario.click(screen.getByRole('link', { name: /Volver a la política/ }))
    expect(screen.getByRole('dialog', { name: '¿Salir sin publicar?' })).toBeInTheDocument()
  })
})
