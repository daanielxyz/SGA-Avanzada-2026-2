// El IVA de la estancia lo decide cada alojamiento (DEC-133 · DEC-141): se suma a la tarifa o ya va dentro. La web
// muestra siempre lo que se paga y, si hay IVA, cuánto es alojamiento y cuánto impuesto.
import type { Precio } from '../api/apartamentos'

/** Lo que se paga por las noches: con su IVA si el alojamiento lo cobra. */
export function valorEstancia(precio: Precio) {
  return precio.totalConIva ?? precio.total
}

/** Si el precio lleva líneas de IVA que mostrar: el alojamiento lo cobra, aunque esta reserva esté exenta. */
export function llevaIva(precio: Precio) {
  return (precio.ivaPorcentaje ?? 0) > 0
}

/**
 * Una tarifa por adulto y noche como la paga el huésped: si el IVA se suma, con él; si ya va dentro, tal cual. Se
 * redondea al peso, como el backend.
 */
export function tarifaConIva(precio: Precio, tarifa: number) {
  const porcentaje = precio.ivaPorcentaje ?? 0
  return porcentaje > 0 && !precio.ivaIncluido ? tarifa + Math.round((tarifa * porcentaje) / 100) : tarifa
}
