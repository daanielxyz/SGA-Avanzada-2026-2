import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Estadia } from '../api/apartamentos'
import { fechaDesdeHoy, masDias } from '../formato'
import CalendarioDisponibilidad from './CalendarioDisponibilidad'

const hoy = fechaDesdeHoy()
const dia = (n: number) => masDias(hoy, n)
// Las noches de dentro de 10 y 11 días ya están reservadas
const OCUPADAS = new Set([dia(10), dia(11)])

/** La API responde cada noche pedida: libre, salvo las ocupadas. */
function laApi({ falla = false } = {}) {
  const fetch = vi.fn((ruta: string) => {
    if (falla) return Promise.reject(new TypeError('sin red'))
    const consulta = new URLSearchParams(ruta.split('?')[1])
    const noches = []
    for (let d = consulta.get('desde') as string; d < (consulta.get('hasta') as string); d = masDias(d, 1)) {
      noches.push({ fecha: d, libre: !OCUPADAS.has(d) })
    }
    return Promise.resolve(new Response(JSON.stringify(noches), { status: 200 }))
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

let ultima: Estadia | null = null

/** La página real guarda las fechas en la dirección; aquí basta con un estado. */
function Pagina({ inicial }: { inicial: Estadia }) {
  const [estadia, setEstadia] = useState(inicial)
  return (
    <>
      <output data-testid="estadia">{`${estadia.entrada} → ${estadia.salida}`}</output>
      <CalendarioDisponibilidad codigo="APT-101" estadia={estadia} alCambiar={(e) => {
        ultima = e
        setEstadia(e)
      }} />
    </>
  )
}

function abrir(inicial: Estadia = { entrada: dia(1), salida: dia(2) }) {
  ultima = null
  const vista = render(<Pagina inicial={inicial} />)
  const boton = (fecha: string) => vista.container.querySelector(`[data-fecha="${fecha}"]`) as HTMLButtonElement
  return { boton }
}

/** Espera a que el calendario sepa cómo está esa noche. */
async function listo(boton: (fecha: string) => HTMLButtonElement, fecha: string) {
  await waitFor(() => expect(boton(fecha)).not.toHaveAccessibleName(/cargando/))
}

afterEach(() => vi.unstubAllGlobals())

describe('Calendario de noches libres', () => {
  it('pide las noches de los dos meses que se ven y tacha las que no están libres', async () => {
    const fetch = laApi()
    const { boton } = abrir()
    await listo(boton, dia(10))

    expect(screen.getAllByRole('table')).toHaveLength(2)
    const mes = screen.getAllByRole('table')[0].querySelector('caption')?.textContent
    expect(mes).toMatch(/^[A-ZÁÉÍÓÚ][a-záéíóú]+ de \d{4}$/)
    expect(boton(dia(10))).toHaveAccessibleName(/no disponible/)
    expect(boton(dia(10))).toHaveClass('no-libre')
    expect(boton(dia(3))).toHaveAccessibleName(/libre/)
    expect(fetch.mock.calls[0][0]).toMatch(new RegExp(`^/api/apartamentos/APT-101/calendario\\?desde=${hoy}&hasta=`))
  })

  it('un clic elige la llegada y el siguiente la salida', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(3))

    await userEvent.click(boton(dia(3)))
    expect(ultima).toEqual({ entrada: dia(3), salida: dia(4) })
    expect(screen.getByText(/Ahora elige la salida/)).toBeInTheDocument()

    await userEvent.click(boton(dia(6)))
    expect(ultima).toEqual({ entrada: dia(3), salida: dia(6) })
    expect(boton(dia(4))).toHaveClass('en-rango')
    expect(boton(dia(3))).toHaveAttribute('aria-pressed', 'true')
    expect(boton(dia(6))).toHaveAttribute('aria-pressed', 'true')
  })

  it('la salida puede caer en la primera noche ocupada, pero no después: no se vende una noche ajena', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(8))

    await userEvent.click(boton(dia(8)))
    expect(boton(dia(10))).toHaveAccessibleName(/solo salida/)
    expect(boton(dia(12))).toHaveAttribute('aria-disabled', 'true')
    await userEvent.click(boton(dia(12)))
    expect(ultima).toEqual({ entrada: dia(8), salida: dia(9) })

    await userEvent.click(boton(dia(10)))
    expect(ultima).toEqual({ entrada: dia(8), salida: dia(10) })
  })

  it('una noche ocupada o pasada no se elige como llegada', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(10))

    await userEvent.click(boton(dia(10)))
    const ayer = boton(dia(-1)) // solo se ve si es del mes actual
    if (ayer) await userEvent.click(ayer)

    expect(ultima).toBeNull()
  })

  it('elegir un día antes de la llegada empieza de nuevo', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(6))

    await userEvent.click(boton(dia(6)))
    await userEvent.click(boton(dia(3)))

    expect(ultima).toEqual({ entrada: dia(3), salida: dia(4) })
    expect(screen.getByText(/Ahora elige la salida/)).toBeInTheDocument()
  })

  it('si las fechas cambian desde los campos, vuelve a pedir la llegada', async () => {
    laApi()
    function ConCampos() {
      const [estadia, setEstadia] = useState<Estadia>({ entrada: dia(1), salida: dia(2) })
      return (
        <>
          <button type="button" onClick={() => setEstadia({ entrada: dia(4), salida: dia(5) })}>
            Desde los campos
          </button>
          <CalendarioDisponibilidad codigo="APT-101" estadia={estadia} alCambiar={setEstadia} />
        </>
      )
    }
    const vista = render(<ConCampos />)
    const boton = (fecha: string) => vista.container.querySelector(`[data-fecha="${fecha}"]`) as HTMLButtonElement
    await listo(boton, dia(3))

    await userEvent.click(boton(dia(3)))
    expect(screen.getByText(/Ahora elige la salida/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Desde los campos' }))

    expect(screen.getByText(/Elige la llegada y luego la salida/)).toBeInTheDocument()
  })

  it('con el teclado, las flechas mueven el foco de día en día y Enter elige', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(3))

    boton(dia(1)).focus()
    await userEvent.keyboard('{ArrowRight}{ArrowRight}')
    expect(boton(dia(3))).toHaveFocus()
    await userEvent.keyboard('{ArrowDown}')
    expect(boton(dia(10))).toHaveFocus()
    await userEvent.keyboard('{ArrowUp}{Enter}')

    expect(ultima).toEqual({ entrada: dia(3), salida: dia(4) })
  })

  it('solo un día recibe el tabulador, para no recorrer el mes entero', async () => {
    laApi()
    const { boton } = abrir()
    await listo(boton, dia(1))

    const conTabulador = [...document.querySelectorAll('[data-fecha]')]
      .filter((b) => b.getAttribute('tabindex') === '0')
    expect(conTabulador).toEqual([boton(dia(1))])
  })

  it('no deja ir antes del mes actual, y al avanzar pide las noches de los meses nuevos', async () => {
    const fetch = laApi()
    const { boton } = abrir()
    await listo(boton, dia(1))

    expect(screen.getByRole('button', { name: /Mes anterior/ })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: /Mes siguiente/ }))

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    expect(screen.getByRole('button', { name: /Mes anterior/ })).toBeEnabled()
  })

  it('si no carga, lo dice sin bloquear los campos de fecha', async () => {
    laApi({ falla: true })
    abrir()

    expect(await screen.findByText(/No pudimos cargar las noches libres/)).toBeInTheDocument()
  })
})
