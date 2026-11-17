import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import FondoEntrada from './FondoEntrada'

/** La pantalla responde a las consultas de CSS que se le digan; a las demás, que no. */
function pantallaCon(...cumplidas: string[]) {
  vi.stubGlobal('matchMedia', (consulta: string) => ({
    matches: cumplidas.includes(consulta),
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  }))
  return vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue()
}

const ANCHA = '(min-width: 800px)'
const SIN_MOVIMIENTO = '(prefers-reduced-motion: reduce)'
const video = () => document.querySelector('video')

afterEach(() => vi.restoreAllMocks())

describe('fondo de las pantallas sin sesión', () => {
  it('en el computador pone el video en silencio, en bucle y con su póster debajo, como decoración', () => {
    const reproducir = pantallaCon(ANCHA)
    render(<FondoEntrada />)

    const elemento = video() as HTMLVideoElement
    expect(elemento).toHaveAttribute('src', '/video/entrada.mp4')
    expect(elemento.autoplay).toBe(true)
    expect(elemento.loop).toBe(true)
    expect(elemento.muted).toBe(true)
    expect(elemento).toHaveAttribute('playsinline')
    expect(elemento).toHaveAttribute('aria-hidden', 'true')
    expect(reproducir).toHaveBeenCalled()
    expect(document.querySelector('img.entrada-fondo')).toHaveAttribute('src', '/video/entrada.jpg')
    expect(screen.queryByText('Cada detalle bajo control.')).not.toBeInTheDocument()
  })

  it('se reproduce siempre, también con «reducir movimiento», y no tiene botón de pausa', () => {
    const reproducir = pantallaCon(ANCHA, SIN_MOVIMIENTO)
    render(<FondoEntrada />)

    const elemento = video() as HTMLVideoElement
    expect(elemento.autoplay).toBe(true)
    expect(reproducir).toHaveBeenCalled()
    fireEvent.playing(elemento)
    expect(elemento).toHaveClass('listo')
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('en el celular no se ve el fondo: ni el video ni el póster se piden', () => {
    pantallaCon()
    render(<FondoEntrada />)

    expect(video()).not.toBeInTheDocument()
    expect(document.querySelector('img')).not.toBeInTheDocument()
  })

  it('si el video o el póster no están, se quitan y queda el fondo de siempre', () => {
    pantallaCon(ANCHA)
    render(<FondoEntrada />)

    fireEvent.error(video() as HTMLVideoElement)
    fireEvent.error(document.querySelector('img.entrada-fondo') as HTMLImageElement)

    expect(video()).not.toBeInTheDocument()
    expect(document.querySelector('img')).not.toBeInTheDocument()
  })
})
