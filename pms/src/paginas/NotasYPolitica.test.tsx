import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { Nota } from '../api/notas'
import type { Reserva } from '../api/reservas'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RUTA = '/api/reservas/RES-2026-00042'

const PENDIENTE: Nota = {
  id: 'NOT-2', texto: 'El señor Mejía pidió cobijas extra para la noche.', autor: 'USR-1',
  autorNombre: 'María Constanza Rodríguez de la Hoz', escritaEn: '2026-11-12T15:05:00', atendida: false, atencion: null,
}
const ATENDIDA: Nota = {
  id: 'NOT-1', texto: 'Falta la cédula de Julián.', autor: 'USR-1', autorNombre: 'María Constanza Rodríguez de la Hoz',
  escritaEn: '2026-11-10T09:15:00', atendida: true,
  atencion: { autor: 'USR-2', autorNombre: 'Andrés Felipe Gómez', fechaHora: '2026-11-11T08:00:00' },
}

const POLITICA = {
  id: 'POL-3', version: 1,
  tramos: [
    { antelacionMinHoras: 48, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 0, montoFijo: null } },
    { antelacionMinHoras: 0, penalizacion: { base: 'VALOR_TOTAL', porcentaje: 50, montoFijo: null } },
  ],
  penalizacionNoShow: { base: 'VALOR_TOTAL', porcentaje: 100, montoFijo: null },
}

const RECHAZO = (motivo: string, mensaje: string): [number, unknown] => [
  409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje, detalles: [], motivo },
]

/** API simulada que recuerda las notas. */
function apiCon({ reserva = {}, notas = [PENDIENTE, ATENDIDA], rechazar = {} }: {
  reserva?: Partial<Reserva>
  notas?: Nota[]
  rechazar?: Record<string, [number, unknown]>
} = {}) {
  const estado = { notas }
  return laApiRespondePorRuta({
    [RUTA]: [200, reservaDePrueba({ estado: 'EN_CURSO', ...reserva })],
    '/api/politicas/POL-3': [200, POLITICA],
    [`${RUTA}/notas`]: (metodo) => {
      if (metodo === 'POST') {
        if (rechazar.escribir) return rechazar.escribir
        estado.notas = [{ ...PENDIENTE, id: 'NOT-3', texto: 'Pidieron cobijas extra', escritaEn: '2026-11-12T16:00:00' }, ...estado.notas]
      }
      return [200, estado.notas]
    },
    [`${RUTA}/notas/NOT-2/atender`]: () => {
      if (rechazar.atender) return rechazar.atender
      estado.notas = estado.notas.map((nota) => (nota.id === 'NOT-2'
        ? { ...nota, atendida: true, atencion: { autor: 'USR-1', autorNombre: 'Ana Gómez', fechaHora: '2026-11-12T16:10:00' } }
        : nota))
      return [200, estado.notas]
    },
  })
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
  return userEvent.setup()
}

describe('notas del turno (BL-08 · DEC-105)', () => {
  it('muestra arriba las pendientes y abajo, plegadas, las atendidas con quién y cuándo', async () => {
    apiCon()
    abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    expect(await within(notas).findByText('Pendientes (1)')).toBeInTheDocument()
    expect(within(notas).getByText('María Constanza Rodríguez de la Hoz · 12 nov 2026, 3:05 p. m.')).toBeInTheDocument()
    expect(within(notas).getByText('Atendidas (1)')).toBeInTheDocument()
    expect(within(notas).getByText(/Atendida por Andrés Felipe Gómez · 11 nov 2026, 8:00 a\. m\./)).toBeInTheDocument()
  })

  it('agrega una nota y la deja arriba; vacía, avisa junto al campo', async () => {
    const fetch = apiCon()
    const usuario = abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    await usuario.click(await within(notas).findByRole('button', { name: 'Guardar nota' }))
    expect(within(notas).getByText('Escribe la nota antes de guardarla.')).toBeInTheDocument()

    await usuario.type(within(notas).getByLabelText('Agregar nota'), 'Pidieron cobijas extra')
    expect(within(notas).getByText('22 / 500')).toBeInTheDocument()
    await usuario.click(within(notas).getByRole('button', { name: 'Guardar nota' }))

    expect(await within(notas).findByText('Pendientes (2)')).toBeInTheDocument()
    expect(within(notas).getByLabelText('Agregar nota')).toHaveValue('')
    const envio = fetch.mock.calls.find(([url, opciones]) => url === `${RUTA}/notas` && opciones?.method === 'POST')
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({ texto: 'Pidieron cobijas extra' })
  })

  it('un rechazo al guardar sale arriba sin perder lo escrito', async () => {
    apiCon({ rechazar: { escribir: RECHAZO('ESTADO_NO_PERMITIDO', 'La reserva terminó') } })
    const usuario = abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    await usuario.type(await within(notas).findByLabelText('Agregar nota'), 'Pidieron cobijas extra')
    await usuario.click(within(notas).getByRole('button', { name: 'Guardar nota' }))

    expect(await within(notas).findByText('Error · No se guardó la nota')).toBeInTheDocument()
    expect(within(notas).getByLabelText('Agregar nota')).toHaveValue('Pidieron cobijas extra')
  })

  it('marcar como atendida la baja a las atendidas', async () => {
    apiCon()
    const usuario = abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    await usuario.click(await within(notas).findByRole('button', { name: /^Marcar como atendida: El señor Mejía/ }))
    expect(await within(notas).findByText('Atendidas (2)')).toBeInTheDocument()
    expect(within(notas).getByText('Sin notas pendientes.')).toBeInTheDocument()
  })

  it('si otro turno ya la atendió, lo dice junto a la nota', async () => {
    apiCon({ rechazar: { atender: RECHAZO('YA_ATENDIDA', 'La nota NOT-2 ya fue atendida') } })
    const usuario = abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    await usuario.click(await within(notas).findByRole('button', { name: /^Marcar como atendida/ }))
    expect(await within(notas).findByRole('alert')).toHaveTextContent('Otro turno ya la marcó como atendida.')
  })

  it('una reserva terminada muestra sus notas sin agregar ni atender', async () => {
    apiCon({ reserva: { estado: 'FINALIZADA' } })
    abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    expect(await within(notas).findByText('Pendientes (1)')).toBeInTheDocument()
    expect(within(notas).queryByLabelText('Agregar nota')).not.toBeInTheDocument()
    expect(within(notas).queryByRole('button', { name: /Marcar como atendida/ })).not.toBeInTheDocument()
  })

  it('sin notas lo dice', async () => {
    apiCon({ notas: [] })
    abrir()

    const notas = await screen.findByRole('region', { name: 'Notas del turno' })
    expect(await within(notas).findByText('Todavía no hay notas.')).toBeInTheDocument()
  })
})

describe('política y no-show en el detalle (DEC-103 · DEC-104)', () => {
  it('resume la política en una línea y «Ver política» muestra sus tramos', async () => {
    apiCon()
    const usuario = abrir()

    expect(await screen.findByText('Retiene el 50 % del valor total si se cancela con menos de 48 horas')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Ver política' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Política de la reserva' })
    expect(within(dialogo).getByText('48 horas o más antes de la entrada')).toBeInTheDocument()
    expect(within(dialogo).getByText('Se retiene el 100 % del valor total')).toBeInTheDocument()
  })

  it('una reserva no-show dice quién la declaró y cuándo', async () => {
    apiCon({
      reserva: {
        estado: 'NO_SHOW',
        noShow: { autor: 'USR-2', autorNombre: 'Andrés Felipe Gómez', fechaHora: '2026-11-12T22:05:00' },
      },
    })
    abrir()

    expect(await screen.findByText('Declarado por Andrés Felipe Gómez el 12 nov 2026, 10:05 p. m.')).toBeInTheDocument()
  })
})
