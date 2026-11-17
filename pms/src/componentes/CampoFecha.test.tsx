import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CampoFecha from './CampoFecha'
import Dialogo from './Dialogo'

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-16T12:00:00') })
})
afterEach(() => {
  vi.useRealTimers()
})

/** Un campo de fecha como lo usan las pantallas: con su etiqueta y su estado. */
function Campo({ inicial = '', alCambiar = () => undefined, ...resto }: {
  inicial?: string
  alCambiar?: (valor: string) => void
  min?: string
  max?: string
  type?: 'date' | 'datetime-local'
}) {
  const [valor, setValor] = useState(inicial)
  return (
    <>
      <label htmlFor="fecha">Fecha de nacimiento</label>
      <CampoFecha etiqueta="Fecha de nacimiento" id="fecha" className="campo-control" value={valor} {...resto}
        onChange={(evento) => {
          setValor(evento.target.value)
          alCambiar(evento.target.value)
        }} />
    </>
  )
}

const calendario = () => screen.getByRole('dialog', { name: 'Elegir fecha de nacimiento' })

describe('campo de fecha con el calendario del PMS', () => {
  it('elegir un día lo escribe en el campo, avisa con su onChange y cierra devolviendo el foco', async () => {
    const alCambiar = vi.fn()
    render(<Campo alCambiar={alCambiar} />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Calendario: Fecha de nacimiento' }))
    await usuario.click(within(calendario()).getByRole('button', { name: /^jueves, 19 de noviembre/ }))

    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveValue('2026-11-19')
    expect(alCambiar).toHaveBeenCalledWith('2026-11-19')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveFocus()
  })

  it('la fecha se sigue pudiendo escribir', async () => {
    const alCambiar = vi.fn()
    render(<Campo alCambiar={alCambiar} />)

    await userEvent.setup().type(screen.getByLabelText('Fecha de nacimiento'), '1990-05-02')

    expect(alCambiar).toHaveBeenLastCalledWith('1990-05-02')
  })

  it('el mes y el año se eligen en listas, para ir lejos sin pasar mes a mes', async () => {
    render(<Campo max="2026-11-16" />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Calendario: Fecha de nacimiento' }))
    await usuario.selectOptions(within(calendario()).getByRole('combobox', { name: 'Año' }), '1990')
    await usuario.selectOptions(within(calendario()).getByRole('combobox', { name: 'Mes' }), 'Mayo')
    await usuario.click(within(calendario()).getByRole('button', { name: /^miércoles, 2 de mayo/ }))

    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveValue('1990-05-02')
  })

  it('los días fuera del mínimo y el máximo no se eligen', async () => {
    render(<Campo min="2026-11-10" max="2026-11-16" />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Calendario: Fecha de nacimiento' }))
    const dia = within(calendario()).getByRole('button', { name: /^martes, 17 de noviembre/ })
    expect(dia).toHaveAttribute('aria-disabled', 'true')
    await usuario.click(dia)

    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveValue('')
    expect(calendario()).toBeInTheDocument()
  })

  it('con hora, elegir un día conserva la hora escrita', async () => {
    render(<Campo type="datetime-local" inicial="2026-11-12T15:30" />)
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Calendario: Fecha de nacimiento' }))
    await usuario.click(within(calendario()).getByRole('button', { name: /^viernes, 13 de noviembre/ }))

    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveValue('2026-11-13T15:30')
  })

  it('dentro de un diálogo, Escape cierra solo el calendario', async () => {
    const alCerrar = vi.fn()
    render(
      <Dialogo titulo="Registrar pago" alCerrar={alCerrar}>
        <div className="dialogo-cuerpo"><Campo /></div>
      </Dialogo>,
    )
    const usuario = userEvent.setup()

    await usuario.click(screen.getByRole('button', { name: 'Calendario: Fecha de nacimiento' }))
    await usuario.keyboard('{Escape}')

    expect(screen.queryByRole('dialog', { name: 'Elegir fecha de nacimiento' })).not.toBeInTheDocument()
    expect(alCerrar).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Fecha de nacimiento')).toHaveFocus()
  })
})
