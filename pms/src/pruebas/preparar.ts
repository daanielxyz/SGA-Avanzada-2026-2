// Se carga antes de cada prueba: agrega comprobaciones como toBeInTheDocument() y limpia la pantalla, la API
// simulada y la sesión guardada entre pruebas.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  localStorage.clear()
})

// jsdom todavía no trae las ventanas modales del navegador
HTMLDialogElement.prototype.showModal ??= function showModal(this: HTMLDialogElement) {
  this.setAttribute('open', '')
}
HTMLDialogElement.prototype.close ??= function close(this: HTMLDialogElement) {
  this.removeAttribute('open')
}
