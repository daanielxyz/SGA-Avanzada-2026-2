// Contraste de los colores de la marca. La API no lo revisa al guardar: la pantalla lo mide para que el administrador
// sepa si la web de reservas se va a leer bien.
import type { NombreColor } from '../api/alojamiento'

/** Lo mínimo para que un texto normal se lea bien (WCAG 2.2, criterio 1.4.3). */
export const CONTRASTE_MINIMO = 4.5

export const COLOR_VALIDO = /^#[0-9A-Fa-f]{6}$/

function luminancia(color: string): number {
  const canal = (inicio: number) => {
    const valor = Number.parseInt(color.slice(inicio, inicio + 2), 16) / 255
    return valor <= 0.039_28 ? valor / 12.92 : ((valor + 0.055) / 1.055) ** 2.4
  }
  return 0.2126 * canal(1) + 0.7152 * canal(3) + 0.0722 * canal(5)
}

/** De 1 (iguales) a 21 (negro sobre blanco). Nulo si alguno de los dos no es un `#RRGGBB`. */
export function contraste(texto: string, fondo: string): number | null {
  if (!COLOR_VALIDO.test(texto) || !COLOR_VALIDO.test(fondo)) return null
  const [clara, oscura] = [luminancia(texto), luminancia(fondo)].sort((a, b) => b - a)
  return (clara + 0.05) / (oscura + 0.05)
}

/** «9,2:1». Se corta, no se redondea: 4,49 no puede decir «4,5:1 · se lee bien». */
export function textoContraste(valor: number): string {
  return `${(Math.floor(valor * 10) / 10).toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}:1`
}

/** Los pares de texto y fondo que de verdad se usan juntos en la web de reservas. */
export const PARES: readonly { texto: NombreColor; fondo: NombreColor; nombre: string }[] = [
  { texto: 'texto', fondo: 'fondo', nombre: 'Texto sobre fondo' },
  { texto: 'textoSuave', fondo: 'fondo', nombre: 'Texto suave sobre fondo' },
  { texto: 'texto', fondo: 'superficie', nombre: 'Texto sobre superficie' },
  { texto: 'textoSuave', fondo: 'superficie', nombre: 'Texto suave sobre superficie' },
  { texto: 'textoSobreAcento', fondo: 'acento', nombre: 'Texto sobre acento, en el acento' },
  { texto: 'peligro', fondo: 'fondo', nombre: 'Peligro sobre fondo' },
  { texto: 'exito', fondo: 'fondo', nombre: 'Éxito sobre fondo' },
]
