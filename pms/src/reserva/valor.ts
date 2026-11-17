// El valor de una estancia con su IVA (DEC-134 · DEC-141). La API manda las noches (`total`), el alojamiento sin IVA,
// el IVA y el total con IVA; una respuesta anterior al IVA solo trae `total`, y entonces la estancia no tiene IVA.
import type { Valor } from '../api/reservas'

/** Lo que el huésped ve como valor de la estancia: las noches con su IVA. */
export function totalConIva(valor: Valor): number {
  return valor.totalConIva ?? valor.total
}

/** Cómo se muestra el IVA de la estancia, o nulo si el alojamiento no lo cobraba al crearla. */
export function lineaIva(valor: Valor): { texto: string; valor: number } | null {
  const porcentaje = valor.ivaPorcentaje ?? 0
  if (porcentaje <= 0) return null
  if (valor.ivaExento) return { texto: `IVA (${porcentaje} %) · exento por pasaporte`, valor: 0 }
  return { texto: `IVA (${porcentaje} %)`, valor: valor.iva ?? 0 }
}

/**
 * Las noches sin el IVA que traen dentro las tarifas (DEC-141): lo que se cobra de alojamiento. Si el IVA se suma, o
 * la respuesta es anterior, son las noches tal cual.
 */
export function valorAlojamiento(valor: Valor): number {
  return valor.alojamiento ?? valor.total
}
