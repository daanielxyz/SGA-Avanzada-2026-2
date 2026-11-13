import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { Estadia } from '../api/apartamentos'
import { fechaCorta, fechaDesdeHoy, masDias } from '../formato'
import SelectorFechas from './SelectorFechas'

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
function Pagina({ inicial, codigo }: { inicial: Estadia; codigo?: string }) {
  const [estadia, setEstadia] = useState(inicial)
  return (
    <>
      <p>Fuera del calendario</p>
      <SelectorFechas codigo={codigo} estadia={estadia} alCambiar={(e) => {
        ultima = e
        setEstadia(e)
      }} />
    </>
  )
}

function abrir(inicial: Estadia = { entrada: dia(1), salida: dia(2) }, codigo: string | undefined = 'APT-101') {
  ultima = null
  const vista = render(<Pagina inicial={inicial} codigo={codigo} />)
  const boton = (fecha: string) => vista.container.querySelector(`[data-fecha="${fecha}"]`) as HTMLButtonElement
  return { boton }
}

const llegada = () => screen.getByRole('button', { name: /^Llegada/ })
const salida = () => screen.getByRole('button', { name: /^Salida/ })

/** Abre el calendario y espera a que sepa cómo está esa noche. */
async function desplegar(boton: (fecha: string) => HTMLButtonElement, fecha: string) {
  await userEvent.click(llegada())
  await waitFor(() => expect(boton(fecha)).not.toHaveAccessibleName(/cargando/))
}

afterEach(() => vi.unstubAllGlobals())

describe('Selector de fechas', () => {
  it('los dos botones muestran las fechas, y el calendario solo aparece al pulsarlos', async () => {
    laApi()
    abrir()

    // toHaveTextContent ve los espacios que no se parten como espacios normales: se compara igual
    const comoTexto = (fecha: string) => fechaCorta(fecha).replace(/\u00a0/g, ' ')
    expect(llegada()).toHaveTextContent(comoTexto(dia(1)))
    expect(salida()).toHaveTextContent(comoTexto(dia(2)))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await userEvent.click(salida())

    expect(screen.getByRole('dialog', { name: 'Elegir fechas' })).toBeInTheDocument()
    expect(salida()).toHaveAttribute('aria-expanded', 'true')
  })

  it('tacha las noches ocupadas, pedidas al abrirlo', async () => {
    const fetch = laApi()
    const { boton } = abrir()
    expect(fetch).not.toHaveBeenCalled()

    await desplegar(boton, dia(10))

    expect(boton(dia(10))).toHaveAccessibleName(/no disponible/)
    expect(boton(dia(10))).toHaveClass('no-libre')
    expect(fetch.mock.calls[0][0]).toMatch(new RegExp(`^/api/apartamentos/APT-101/calendario\\?desde=${hoy}&hasta=`))
  })

  it('cada vez que se abre vuelve a preguntar, para ver lo que se liberó o se tomó', async () => {
    const fetch = laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(3))
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))

    await desplegar(boton, dia(3))

    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('un clic elige la llegada y deja la salida por elegir; el siguiente elige la salida', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(3))

    await userEvent.click(boton(dia(3)))
    expect(ultima).toEqual({ entrada: dia(3), salida: '' })
    expect(salida()).toHaveTextContent('Agregar fecha')
    expect(screen.getByText(/Ahora elige la salida/)).toBeInTheDocument()

    await userEvent.click(boton(dia(6)))
    expect(ultima).toEqual({ entrada: dia(3), salida: dia(6) })
    expect(screen.getByRole('dialog')).toHaveTextContent('3 noches')
    expect(boton(dia(4))).toHaveClass('en-rango')
  })

  it('la salida puede caer en la primera noche ocupada, pero no después', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(8))

    await userEvent.click(boton(dia(8)))
    expect(boton(dia(10))).toHaveAccessibleName(/solo salida/)
    expect(boton(dia(12))).toHaveAttribute('aria-disabled', 'true')
    await userEvent.click(boton(dia(12)))
    expect(ultima).toEqual({ entrada: dia(8), salida: '' })

    await userEvent.click(boton(dia(10)))
    expect(ultima).toEqual({ entrada: dia(8), salida: dia(10) })
  })

  it('una noche ocupada no se elige como llegada', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(10))

    await userEvent.click(boton(dia(10)))

    expect(ultima).toBeNull()
  })

  it('con las dos fechas puestas, un clic empieza de nuevo', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(5))

    await userEvent.click(boton(dia(5)))

    expect(ultima).toEqual({ entrada: dia(5), salida: '' })
  })

  it('Limpiar fechas deja las dos vacías', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(3))

    await userEvent.click(screen.getByRole('button', { name: 'Limpiar fechas' }))

    expect(ultima).toEqual({ entrada: '', salida: '' })
    expect(llegada()).toHaveTextContent('Agregar fecha')
    expect(screen.getByRole('button', { name: 'Limpiar fechas' })).toBeDisabled()
  })

  it('Cerrar y Escape lo cierran y devuelven el foco al botón; tocar fuera también lo cierra', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(3))

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(llegada()).toHaveFocus()

    await desplegar(boton, dia(3))
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(llegada()).toHaveFocus()

    await desplegar(boton, dia(3))
    await userEvent.click(screen.getByText('Fuera del calendario'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('al abrir, el foco va a la llegada; las flechas lo mueven y Enter elige', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(3))

    expect(boton(dia(1))).toHaveFocus()
    await userEvent.keyboard('{ArrowRight}{ArrowRight}')
    expect(boton(dia(3))).toHaveFocus()
    await userEvent.keyboard('{ArrowDown}')
    expect(boton(dia(10))).toHaveFocus()
    await userEvent.keyboard('{ArrowUp}{Enter}')

    expect(ultima).toEqual({ entrada: dia(3), salida: '' })
  })

  it('solo un día recibe el tabulador, para no recorrer el mes entero', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(1))

    const dialogo = screen.getByRole('dialog')
    const conTabulador = [...dialogo.querySelectorAll('[data-fecha]')].filter((b) => b.getAttribute('tabindex') === '0')
    expect(conTabulador).toEqual([boton(dia(1))])
  })

  it('el mes se escribe con mayúscula inicial, y no deja ir antes del mes actual', async () => {
    laApi()
    const { boton } = abrir()
    await desplegar(boton, dia(1))

    const mes = within(screen.getByRole('dialog')).getAllByRole('table')[0].querySelector('caption')?.textContent
    expect(mes).toMatch(/^[A-ZÁÉÍÓÚ][a-záéíóú]+ de \d{4}$/)
    expect(screen.getByRole('button', { name: /Anterior/ })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: /Siguiente/ }))
    expect(screen.getByRole('button', { name: /Anterior/ })).toBeEnabled()
  })

  it('sin apartamento, como en la búsqueda, no pide nada y todas las noches futuras se eligen', async () => {
    const fetch = laApi()
    const { boton } = abrir(undefined, '') // sin código de apartamento

    await userEvent.click(llegada())
    await userEvent.click(boton(dia(10)))

    expect(fetch).not.toHaveBeenCalled()
    expect(ultima).toEqual({ entrada: dia(10), salida: '' })
  })

  it('si no carga, lo dice', async () => {
    laApi({ falla: true })
    abrir()

    await userEvent.click(llegada())

    expect(await screen.findByText(/No pudimos revisar qué noches están libres/)).toBeInTheDocument()
  })
})
