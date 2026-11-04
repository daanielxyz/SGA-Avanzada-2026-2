import { afterEach, describe, expect, it } from 'vitest'
import { aplicarMarca } from './marca'

const raiz = () => document.documentElement.style

afterEach(() => aplicarMarca(null))

describe('aplicarMarca', () => {
  it('pone los colores, los radios y las fuentes del hotel en las variables de CSS (DEC-83)', () => {
    aplicarMarca({
      colores: { acento: '#F4B942', textoSobreAcento: '#1B3A24' },
      fuentes: { titulos: 'Playfair Display', texto: 'Inter' },
      radio: { mediano: '12px' },
    })

    expect(raiz().getPropertyValue('--acento')).toBe('#F4B942')
    expect(raiz().getPropertyValue('--texto-sobre-acento')).toBe('#1B3A24')
    expect(raiz().getPropertyValue('--radio-mediano')).toBe('12px')
    expect(raiz().getPropertyValue('--fuente-titulos')).toContain('"Playfair Display"')
    expect(document.querySelector<HTMLLinkElement>('#fuentes-marca')?.href)
      .toContain('family=Playfair+Display:wght@400;500;600;700&family=Inter:wght')
  })

  it('un campo que el hotel no define se queda con el valor neutro', () => {
    aplicarMarca({ colores: { acento: '#F4B942' } })

    expect(raiz().getPropertyValue('--fondo')).toBe('')
    expect(document.querySelector('#fuentes-marca')).toBeNull()
  })

  it('un campo que la API envía vacío también se queda con el valor neutro', () => {
    aplicarMarca({ colores: { acento: null, fondo: '#F8F7F1' }, fuentes: { titulos: null, texto: null } })

    expect(raiz().getPropertyValue('--acento')).toBe('')
    expect(raiz().getPropertyValue('--fondo')).toBe('#F8F7F1')
    expect(document.querySelector('#fuentes-marca')).toBeNull()
  })

  it('sin marca vuelve a los valores neutros y quita las fuentes', () => {
    aplicarMarca({ colores: { acento: '#F4B942' }, fuentes: { texto: 'Inter' } })
    aplicarMarca(null)

    expect(raiz().getPropertyValue('--acento')).toBe('')
    expect(document.querySelector('#fuentes-marca')).toBeNull()
  })

  it('el nombre de una fuente no se puede salir del valor de CSS', () => {
    aplicarMarca({ fuentes: { titulos: 'Mala"; color: red' } })

    expect(raiz().getPropertyValue('--fuente-titulos')).toBe('"Mala; color: red", Georgia, serif')
  })
})
