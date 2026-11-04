// Los datos del hotel salen de la API, nunca del código: la misma web sirve a cualquier alojamiento (DEC-83).
import { pedir } from './cliente'

/**
 * La marca de un hotel (BL-05): todo opcional, lo que falte queda con los valores neutros del esqueleto. La API todavía
 * no la envía; llega en `feature/nucleo/marca-alojamiento`.
 */
export interface Marca {
  colores?: {
    fondo?: string
    superficie?: string
    texto?: string
    textoSuave?: string
    borde?: string
    bordeCampo?: string
    acento?: string
    textoSobreAcento?: string
    peligro?: string
    exito?: string
  }
  fuentes?: { titulos?: string; texto?: string }
  radio?: { pequeno?: string; mediano?: string; grande?: string }
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
