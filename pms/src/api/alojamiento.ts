// Datos del alojamiento que necesita el PMS: nada de él va escrito en el código (DEC-83).
import { pedir } from './cliente'

/** Lo que usa el PMS de `AlojamientoResponse`; el logo y los colores de marca todavía no están en la API (BL-05). */
export interface Alojamiento {
  nombre: string
}

/** `GET /api/alojamiento` (público). */
export function consultarAlojamiento(): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento')
}
