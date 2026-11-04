// Datos del alojamiento que necesita el PMS: nada de él va escrito en el código (DEC-83).
import { pedir } from './cliente'

/** Lo que usa el PMS de `AlojamientoResponse`. */
export interface Alojamiento {
  nombre: string
  parametros: {
    /** 0 = no se exige anticipo. */
    anticipoPct: number
    /** Desde esta hora del día de entrada se puede declarar el no-show (RES-16). */
    horaLimiteNoShow: string
  }
  mediosPago: string[]
}

/** `GET /api/alojamiento` (público). */
export function consultarAlojamiento(): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento')
}
