import { describe, expect, it, vi } from 'vitest'
import { aplicarTema, CLAVE_TEMA, elegirTema, leerTema } from './tema'

describe('tema del PMS (día y noche)', () => {
  it('sin nada guardado, o con un valor que no conoce, es noche', () => {
    expect(leerTema()).toBe('noche')
    localStorage.setItem(CLAVE_TEMA, 'fucsia')
    expect(leerTema()).toBe('noche')
  })

  it('elegir día lo pone en la página y lo recuerda; volver a noche lo quita', () => {
    elegirTema('dia')
    expect(document.documentElement.dataset.tema).toBe('dia')
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('dia')
    expect(leerTema()).toBe('dia')

    elegirTema('noche')
    expect(document.documentElement.dataset.tema).toBeUndefined()
    expect(leerTema()).toBe('noche')
  })

  it('avisa al navegador del esquema y del color de su barra', () => {
    document.head.innerHTML += '<meta name="theme-color" content="#06151e"><meta name="color-scheme" content="dark">'
    aplicarTema('dia')
    expect(document.querySelector('meta[name="color-scheme"]')).toHaveAttribute('content', 'light')
    expect(document.querySelector('meta[name="theme-color"]')).toHaveAttribute('content', '#b8a078')
    aplicarTema('noche')
    expect(document.querySelector('meta[name="color-scheme"]')).toHaveAttribute('content', 'dark')
    document.querySelectorAll('meta[name="theme-color"], meta[name="color-scheme"]').forEach((meta) => meta.remove())
  })

  it('si el navegador no deja usar el almacenamiento, no falla: lee noche y el cambio vale por esta visita', () => {
    const leer = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })
    const guardar = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })

    expect(leerTema()).toBe('noche')
    expect(() => elegirTema('dia')).not.toThrow()
    expect(document.documentElement.dataset.tema).toBe('dia')

    leer.mockRestore()
    guardar.mockRestore()
  })
})
