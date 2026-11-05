import type { Estadia } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'

/** Lo obvio antes de llamar a la API; las reglas de verdad las revisa el backend. Vacío si está bien. */
export function problemaDeEstadia({ entrada, salida }: Estadia) {
  // Ninguna se supone: sin las dos no se busca ni se reserva
  if (!entrada || !salida) {
    return 'Elige la llegada y la salida.'
  }
  // Las fechas AAAA-MM-DD se pueden comparar como texto
  if (entrada < fechaDesdeHoy()) {
    return 'La llegada no puede ser antes de hoy.'
  }
  if (salida <= entrada) {
    return 'La salida debe ser al menos un día después de la llegada.'
  }
  return null
}
