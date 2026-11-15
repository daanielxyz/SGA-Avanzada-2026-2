import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from '../App'
import type { CorreccionDocumento } from '../api/reservas'
import { reservaDePrueba } from '../pruebas/reservasDePrueba'
import { entrarComo, laApiRespondePorRuta } from '../pruebas/sesionDePrueba'
import ProveedorSesion from '../sesion/ProveedorSesion'

const RUTA = '/api/reservas/RES-2026-00042'

const correccion = (cambios: Partial<CorreccionDocumento> = {}): CorreccionDocumento => ({
  ocupante: 'Luis Ríos', tipoAnterior: 'CC', numeroAnterior: '1094', tipoNuevo: 'TI', numeroNuevo: '1094000',
  motivo: 'Se escribió mal al reservar', autor: 'USR-2', autorNombre: 'María Fernanda Ospina',
  fechaHora: '2026-11-13T21:30:00', titular: false, ...cambios,
})

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

const cuerpos = (fetch: ReturnType<typeof laApiRespondePorRuta>) => fetch.mock.calls
  .filter(([, opciones]) => (opciones as RequestInit | undefined)?.method === 'PUT')
  .map(([url, opciones]) => ({ url, cuerpo: JSON.parse(String((opciones as RequestInit).body)) }))

describe('corregir documento (DEC-120)', () => {
  it('corrige el de un huésped con su motivo, avisa los rechazos y deja la corrección en el historial', async () => {
    let intentos = 0
    const fetch = laApiRespondePorRuta({
      [RUTA]: [200, reservaDePrueba({ estado: 'CONFIRMADA' })],
      [`${RUTA}/ocupantes/OCU-2/documento`]: () => (intentos++ === 0
        ? [409, { estado: 409, codigo: 'REGLA_NEGOCIO', mensaje: 'Ese documento ya lo tiene Ana Ríos en esta reserva', detalles: [] }]
        : [200, reservaDePrueba({
          estado: 'CONFIRMADA', correccionesDocumento: [correccion()],
          ocupantes: reservaDePrueba().ocupantes.map((o) => (o.id === 'OCU-2' ? { ...o, tipoDocumento: 'TI', numeroDocumento: '1094000' } : o)),
        })]),
    })
    const persona = abrir()
    const ocupantes = await screen.findByRole('table', { name: 'Ocupantes (2)' })
    await persona.click(within(ocupantes).getByRole('button', { name: 'Corregir el documento de Luis Ríos' }))
    const dialogo = screen.getByRole('dialog', { name: 'Corregir documento' })
    expect(dialogo).toHaveTextContent('CC 1094')

    // Luis tiene 10 años: la cédula ya no se ofrece, y el mismo documento no es una corrección
    await persona.type(within(dialogo).getByLabelText('Número nuevo'), '1094')
    await persona.click(within(dialogo).getByRole('button', { name: 'Corregir documento' }))
    expect(within(dialogo).getByText('Escribe por qué se corrige.')).toBeInTheDocument()
    expect(within(dialogo).getByText(/Un menor de edad no tiene cédula/)).toBeInTheDocument()

    await persona.selectOptions(within(dialogo).getByLabelText('Tipo nuevo'), 'TI')
    await persona.clear(within(dialogo).getByLabelText('Número nuevo'))
    await persona.type(within(dialogo).getByLabelText('Número nuevo'), ' 1094000 ')
    await persona.type(within(dialogo).getByLabelText(/Motivo/), 'Se escribió mal al reservar')
    await persona.click(within(dialogo).getByRole('button', { name: 'Corregir documento' }))
    expect(await within(dialogo).findByText('Ese documento ya lo tiene Ana Ríos en esta reserva')).toBeInTheDocument()

    await persona.click(within(dialogo).getByRole('button', { name: 'Corregir documento' }))
    expect(await screen.findByText('Documento corregido')).toBeInTheDocument()
    expect(cuerpos(fetch)[1]).toEqual({
      url: `${RUTA}/ocupantes/OCU-2/documento`,
      cuerpo: { tipoDocumento: 'TI', numeroDocumento: '1094000', motivo: 'Se escribió mal al reservar' },
    })
    const historial = screen.getByRole('region', { name: 'Documentos corregidos (1)' })
    expect(historial).toHaveTextContent('Luis Ríos')
    expect(historial).toHaveTextContent('Antes: CC 1094')
    expect(historial).toHaveTextContent('ahora: TI 1094000')
    expect(historial).toHaveTextContent('Por María Fernanda Ospina, el 13 nov 2026, 9:30 p. m.')
  })

  it('corrige el del titular leyendo su documento actual, sin registro civil', async () => {
    const fetch = laApiRespondePorRuta({
      [RUTA]: [200, reservaDePrueba({ estado: 'PENDIENTE' })],
      '/api/titulares/TIT-7': [200, { id: 'TIT-7', nombre: 'Ana Ríos', tipoDocumento: 'CC', numeroDocumento: '52481',
        correo: null, telefono: null, validado: false, validadoEn: null, validadoPor: null }],
      [`${RUTA}/titular/documento`]: [200, reservaDePrueba({ estado: 'PENDIENTE', correccionesDocumento: [
        correccion({ ocupante: 'Ana Ríos', tipoAnterior: 'CC', numeroAnterior: '52481', tipoNuevo: 'CC', numeroNuevo: '52418', titular: true }),
      ] })],
    })
    const persona = abrir()
    await persona.click(await screen.findByRole('button', { name: 'Corregir el documento del titular' }))
    const dialogo = screen.getByRole('dialog', { name: 'Corregir documento' })
    expect(await within(dialogo).findByText('CC 52481')).toBeInTheDocument()
    expect(within(dialogo).queryByRole('option', { name: 'Registro civil' })).not.toBeInTheDocument()

    await persona.type(within(dialogo).getByLabelText('Número nuevo'), '52418')
    await persona.type(within(dialogo).getByLabelText(/Motivo/), 'Dos números invertidos')
    await persona.click(within(dialogo).getByRole('button', { name: 'Corregir documento' }))
    const historial = await screen.findByRole('region', { name: 'Documentos corregidos (1)' })
    expect(within(historial).getByText('Titular')).toBeInTheDocument()
    expect(cuerpos(fetch)[0]).toEqual({
      url: `${RUTA}/titular/documento`,
      cuerpo: { tipoDocumento: 'CC', numeroDocumento: '52418', motivo: 'Dos números invertidos' },
    })
  })

  it('una reserva terminada no ofrece corregir, pero muestra las correcciones de antes, la más reciente primero', async () => {
    laApiRespondePorRuta({
      [RUTA]: [200, reservaDePrueba({ estado: 'FINALIZADA', correccionesDocumento: [
        correccion({ motivo: 'Primera' }), correccion({ motivo: 'Segunda', autorNombre: null }),
      ] })],
    })
    abrir()
    const historial = await screen.findByRole('region', { name: 'Documentos corregidos (2)' })
    const items = within(historial).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Segunda')
    expect(items[0]).toHaveTextContent('El 13 nov 2026')
    expect(items[1]).toHaveTextContent('Primera')
    expect(screen.queryByRole('button', { name: /Corregir el documento/ })).not.toBeInTheDocument()
  })
})
