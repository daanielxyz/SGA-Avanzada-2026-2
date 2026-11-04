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
    /** Desde esta edad, a la fecha de entrada, un ocupante paga (RN-06). */
    umbralEdadFacturable: number
    aceptaMascotas: boolean
    limiteMascotas: number
    /** Por mascota, una vez por estancia (RP-03). */
    cargoMascota: number
    /** Vehículos que admite el parqueadero cada noche; 0 = no hay (RP-02). */
    cupoParqueadero: number
  }
  mediosPago: string[]
}

/** `GET /api/alojamiento` (público). */
export function consultarAlojamiento(): Promise<Alojamiento> {
  return pedir<Alojamiento>('/api/alojamiento')
}
