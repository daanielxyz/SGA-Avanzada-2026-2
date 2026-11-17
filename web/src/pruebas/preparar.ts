// Se carga antes de cada prueba: agrega comprobaciones como toBeInTheDocument() y limpia la pantalla entre pruebas.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => cleanup())

// jsdom no mueve la página: sin esto, cada cambio de página avisaría «Not implemented: window.scrollTo»
window.scrollTo = () => {}
