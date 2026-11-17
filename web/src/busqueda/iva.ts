// El IVA de la estancia lo decide cada alojamiento (DEC-133 · DEC-141): se suma a la tarifa o ya va dentro. Mientras
// se comparan apartamentos, los precios van sin IVA; al abrir uno, su precio se desglosa en alojamiento, IVA y total.
import type { Precio } from '../api/apartamentos'

/** Lo que se paga por las noches: con su IVA si el alojamiento lo cobra. */
export function valorEstancia(precio: Precio) {
  return precio.totalConIva ?? precio.total
}

/** Las noches sin IVA: lo que se muestra mientras se comparan apartamentos. */
export function valorSinIva(precio: Precio) {
  return precio.alojamiento ?? precio.total
}

/** Si el precio lleva líneas de IVA que mostrar: el alojamiento lo cobra, aunque esta reserva esté exenta. */
export function llevaIva(precio: Precio) {
  return (precio.ivaPorcentaje ?? 0) > 0
}

/**
 * Una tarifa por adulto y noche sin IVA: si las tarifas ya lo traen, se le quita, redondeado al peso como el
 * backend; si se suma, la tarifa ya es sin IVA.
 */
export function tarifaSinIva(precio: Precio, tarifa: number) {
  const porcentaje = precio.ivaPorcentaje ?? 0
  return porcentaje > 0 && precio.ivaIncluido ? Math.round((tarifa * 100) / (100 + porcentaje)) : tarifa
}
