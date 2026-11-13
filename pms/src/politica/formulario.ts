// El formulario de «Publicar nueva versión»: renglones de texto que se revisan al escribir con las mismas reglas del
// dominio (PoliticaCancelacion, Penalizacion), para que el error salga junto al renglón antes de publicar.
import type { BaseRetencion, Penalizacion, PenalizacionNueva, Politica, PoliticaNueva } from '../api/politicas'

export type Unidad = 'porcentaje' | 'monto'

export interface Retencion {
  valor: string
  unidad: Unidad
  base: BaseRetencion
}

export interface Tramo extends Retencion {
  /** Para que React siga al renglón aunque cambien sus horas. */
  clave: number
  horas: string
}

export interface FormPolitica {
  tramos: Tramo[]
  noShow: Retencion
}

export interface ErroresPolitica {
  tramos: Record<number, { horas?: string; valor?: string }>
  noShow?: string
  /** Lo que es de la lista entera: el tramo desde 0 horas y el mínimo de tramos. */
  lista: string[]
}

export const BASES: readonly (readonly [BaseRetencion, string])[] = [
  ['VALOR_TOTAL', 'el valor total'],
  ['PAGADO', 'lo pagado'],
  ['ANTICIPO_EXIGIDO', 'el anticipo exigido'],
]

let siguienteClave = 1
export function nuevaClave(): number {
  siguienteClave += 1
  return siguienteClave
}

function desdePenalizacion(penalizacion: Penalizacion): Retencion {
  const monto = penalizacion.montoFijo !== null && penalizacion.montoFijo !== undefined
  return {
    valor: String(monto ? penalizacion.montoFijo : (penalizacion.porcentaje ?? 0)),
    unidad: monto ? 'monto' : 'porcentaje',
    base: (penalizacion.base as BaseRetencion) ?? 'VALOR_TOTAL',
  }
}

/** Parte de la vigente, de la mayor antelación a la menor; sin vigente, un tramo desde 0 horas y el no-show. */
export function formDesde(vigente: Politica | null): FormPolitica {
  if (!vigente) {
    return {
      tramos: [{ clave: nuevaClave(), horas: '0', valor: '100', unidad: 'porcentaje', base: 'VALOR_TOTAL' }],
      noShow: { valor: '100', unidad: 'porcentaje', base: 'VALOR_TOTAL' },
    }
  }
  return {
    tramos: [...vigente.tramos]
      .sort((a, b) => b.antelacionMinHoras - a.antelacionMinHoras)
      .map((tramo) => ({ clave: nuevaClave(), horas: String(tramo.antelacionMinHoras), ...desdePenalizacion(tramo.penalizacion) })),
    noShow: desdePenalizacion(vigente.penalizacionNoShow),
  }
}

/** @param miles si acepta puntos de mil («50.000»): solo los montos en pesos */
function entero(texto: string, miles = false): number | null {
  const limpio = miles ? texto.trim().replaceAll('.', '') : texto.trim()
  return /^\d+$/.test(limpio) ? Number(limpio) : null
}

// POL-03 · DEC-41: un porcentaje de 0 a 100 o un monto fijo mayor que 0, en pesos sin decimales
function errorRetencion(retencion: Retencion): string | undefined {
  const valor = entero(retencion.valor, retencion.unidad === 'monto')
  if (retencion.unidad === 'porcentaje') {
    if (valor === null || valor > 100) return 'Escribe un porcentaje entero de 0 a 100.'
    return undefined
  }
  if (valor === null) return 'Escribe el monto en pesos, sin decimales.'
  if (valor === 0) return 'Un monto fijo debe ser mayor que 0. Para no retener nada, usa 0 %.'
  return undefined
}

// POL-01 · POL-02: un tramo desde 0 horas, sin dos tramos en las mismas horas, y el mínimo del alojamiento
export function validar(form: FormPolitica, minimoTramos: number): ErroresPolitica {
  const errores: ErroresPolitica = { tramos: {}, lista: [] }
  const porHoras = new Map<number, number[]>()
  for (const tramo of form.tramos) {
    const propios: { horas?: string; valor?: string } = {}
    const horas = entero(tramo.horas)
    if (horas === null) propios.horas = 'Escribe las horas con un número entero.'
    else porHoras.set(horas, [...(porHoras.get(horas) ?? []), tramo.clave])
    propios.valor = errorRetencion(tramo)
    if (propios.horas || propios.valor) errores.tramos[tramo.clave] = propios
  }
  for (const [horas, claves] of porHoras) {
    if (claves.length < 2) continue
    for (const clave of claves) {
      errores.tramos[clave] = {
        ...errores.tramos[clave],
        horas: `Otro tramo también empieza en ${horas} horas. Cambia las horas de uno de los dos.`,
      }
    }
  }
  if (!porHoras.has(0)) {
    errores.lista.push('Falta el tramo desde 0 horas: pon 0 en las horas de uno de los tramos o agrega uno.')
  }
  if (form.tramos.length < minimoTramos) {
    errores.lista.push(`Hacen falta al menos ${minimoTramos} tramos. Agrega otro tramo.`)
  }
  errores.noShow = errorRetencion(form.noShow)
  return errores
}

export function hayErrores(errores: ErroresPolitica): boolean {
  return errores.lista.length > 0 || Boolean(errores.noShow) || Object.keys(errores.tramos).length > 0
}

function aPenalizacion(retencion: Retencion): PenalizacionNueva {
  const valor = entero(retencion.valor, retencion.unidad === 'monto') ?? 0
  return retencion.unidad === 'porcentaje'
    ? { base: retencion.base, porcentaje: valor }
    : { base: retencion.base, montoFijo: valor }
}

export function cuerpo(form: FormPolitica): PoliticaNueva {
  return {
    tramos: form.tramos.map((tramo) => ({ antelacionMinHoras: entero(tramo.horas) ?? 0, penalizacion: aPenalizacion(tramo) })),
    penalizacionNoShow: aPenalizacion(form.noShow),
  }
}

/** Lo que se publicaría, con la forma de una política, para contarlo igual que «Ver política». */
export function vistaPrevia(form: FormPolitica): Politica {
  const comoPenalizacion = (retencion: Retencion): Penalizacion => {
    const { base, porcentaje, montoFijo } = aPenalizacion(retencion)
    return { base, porcentaje: porcentaje ?? null, montoFijo: montoFijo ?? null }
  }
  return {
    id: 'vista-previa',
    version: 0,
    tramos: form.tramos.map((tramo) => ({ antelacionMinHoras: entero(tramo.horas) ?? 0, penalizacion: comoPenalizacion(tramo) })),
    penalizacionNoShow: comoPenalizacion(form.noShow),
  }
}
