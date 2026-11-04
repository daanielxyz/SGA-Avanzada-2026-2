// Los datos del hotel salen de la API, nunca del código: la misma web sirve a cualquier alojamiento (DEC-83).
import { pedir } from './cliente'

/**
 * `MarcaResponse`: la marca de un hotel (BL-05 · DEC-87). Todo opcional: lo que venga vacío queda con los valores
 * neutros del esqueleto.
 */
export interface Marca {
  colores?: {
    fondo?: string | null
    superficie?: string | null
    texto?: string | null
    textoSuave?: string | null
    borde?: string | null
    bordeCampo?: string | null
    acento?: string | null
    textoSobreAcento?: string | null
    peligro?: string | null
    exito?: string | null
  }
  fuentes?: { titulos?: string | null; texto?: string | null }
  radio?: { pequeno?: string | null; mediano?: string | null; grande?: string | null }
  logoUrl?: string | null
  portadaUrl?: string | null
}

/** De `AlojamientoResponse`, solo lo que usa la web por ahora. */
export interface Alojamiento {
  nombre: string
  descripcion?: string | null
  ciudad?: string | null
  parametros: {
    /** Desde esta edad, cumplida el día de llegada, se paga (RN-06). */
    umbralEdadFacturable: number
  }
  marca?: Marca | null
}

export function consultarAlojamiento() {
  return pedir<Alojamiento>('/api/alojamiento')
}
