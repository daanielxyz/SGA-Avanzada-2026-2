import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'

function abrir(direccion: string) {
  render(
    <MemoryRouter initialEntries={[direccion]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App', () => {
  it('muestra la página de inicio', () => {
    abrir('/')

    expect(screen.getByRole('heading', { name: 'Puerta al Sol' })).toBeInTheDocument()
  })

  it('una dirección que no existe muestra la página de no encontrada', () => {
    abrir('/no-existe')

    expect(screen.getByRole('heading', { name: 'No encontramos esta página' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
