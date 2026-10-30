// Los datos del hotel salen de la API, nunca del código: la misma web sirve a cualquier alojamiento (DEC-83).
import { pedir } from './cliente'

/** De `AlojamientoResponse`, solo lo que usa la web por ahora. */
export interface Alojamiento {
  nombre: string
}

export function consultarAlojamiento() {
  return pedir<Alojamiento>('/api/alojamiento')
}
