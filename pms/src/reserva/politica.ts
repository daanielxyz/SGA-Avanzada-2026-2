// Textos de la política de cancelación para recepción (RN-13 · DEC-104). La API manda tramos por horas de antelación,
// sin un nombre: aquí se dicen en palabras.
import type { Penalizacion, Politica, Tramo } from '../api/politicas'
import { cantidad, dinero } from '../formato'

const BASE: Record<string, string> = {
  VALOR_TOTAL: 'del valor total',
  PAGADO: 'de lo pagado',
  ANTICIPO_EXIGIDO: 'del anticipo exigido',
}

// Un monto fijo nunca pasa de su base (Penalizacion.calcular)
const TOPE: Record<string, string> = {
  VALOR_TOTAL: 'sin pasar del valor total',
  PAGADO: 'sin pasar de lo pagado',
  ANTICIPO_EXIGIDO: 'sin pasar del anticipo exigido',
}

function retiene({ porcentaje, montoFijo }: Penalizacion): boolean {
  return (porcentaje ?? 0) > 0 || (montoFijo ?? 0) > 0
}

/** «el 50 % del valor total» o «$ 100.000». */
function cuanto(penalizacion: Penalizacion): string {
  if (penalizacion.montoFijo !== null && penalizacion.montoFijo !== undefined) return dinero(penalizacion.montoFijo)
  return `el ${penalizacion.porcentaje ?? 0} % ${BASE[penalizacion.base] ?? ''}`.trim()
}

/** «Se retiene el 50 % del valor total», «Se retienen $ 50.000, sin pasar del valor total» o «No se retiene nada». */
export function textoRetencion(penalizacion: Penalizacion): string {
  if (!retiene(penalizacion)) return 'No se retiene nada'
  if (penalizacion.montoFijo !== null && penalizacion.montoFijo !== undefined) {
    const tope = TOPE[penalizacion.base]
    return `Se retienen ${dinero(penalizacion.montoFijo)}${tope ? `, ${tope}` : ''}`
  }
  return `Se retiene ${cuanto(penalizacion)}`
}

/** Los tramos de mayor a menor antelación, que es como se leen. */
export function tramosOrdenados(politica: Politica): Tramo[] {
  return [...politica.tramos].sort((a, b) => b.antelacionMinHoras - a.antelacionMinHoras)
}

/** «48 horas o más antes de la entrada», «Menos de 48 horas antes de la entrada», «Entre 24 y 48 horas antes». */
export function textoTramo(tramos: Tramo[], indice: number): string {
  const minimo = tramos[indice].antelacionMinHoras
  const anterior = indice > 0 ? tramos[indice - 1].antelacionMinHoras : null
  const horas = (valor: number) => cantidad(valor, 'hora', 'horas')
  if (anterior === null) return minimo > 0 ? `${horas(minimo)} o más antes de la entrada` : 'En cualquier momento'
  if (minimo <= 0) return `Menos de ${horas(anterior)} antes de la entrada`
  return `Entre ${minimo} y ${horas(anterior)} antes de la entrada`
}

/** Una línea para el detalle: el primer tramo que retiene. */
export function resumenPolitica(politica: Politica): string {
  const tramos = tramosOrdenados(politica)
  const indice = tramos.findIndex((tramo) => retiene(tramo.penalizacion))
  if (indice < 0) return 'No retiene nada al cancelar'
  const penalizacion = cuanto(tramos[indice].penalizacion)
  if (indice === 0) return `Retiene ${penalizacion} si se cancela`
  return `Retiene ${penalizacion} si se cancela con menos de ${cantidad(tramos[indice - 1].antelacionMinHoras, 'hora', 'horas')}`
}
