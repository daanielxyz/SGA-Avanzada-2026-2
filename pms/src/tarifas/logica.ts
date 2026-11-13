// Lo que deciden las pantallas de temporadas y tarifas sin hablar con la API: cómo se ordenan y se leen las temporadas,
// qué se revisa antes de enviar y qué se envía (pms/DISENO.md, «Sprint 4: temporadas y tarifas»). La API sigue siendo
// quien decide.
import type { NuevaTemporada, Tarifa, Temporada } from '../api/tarifas'
import { dinero, fechaCorta } from '../formato'

export const MAXIMO_NOMBRE_TEMPORADA = 80

/** Las activas en el orden del calendario (la base primero) y las inactivas, de la más reciente a la más antigua. */
export function ordenarTemporadas(temporadas: Temporada[]): { activas: Temporada[]; inactivas: Temporada[] } {
  const inicio = (t: Temporada) => t.fechaInicio ?? ''
  return {
    activas: temporadas.filter((t) => t.activa)
      .sort((a, b) => Number(b.esBase) - Number(a.esBase) || inicio(a).localeCompare(inicio(b))),
    inactivas: temporadas.filter((t) => !t.activa).sort((a, b) => inicio(b).localeCompare(inicio(a))),
  }
}

/** «15 dic 2026 – 15 ene 2027», los dos días incluidos (TEM-01). La base no tiene fechas. */
export function textoFechas(temporada: Pick<Temporada, 'fechaInicio' | 'fechaFin'>): string {
  if (!temporada.fechaInicio || !temporada.fechaFin) return '—'
  return `${fechaCorta(temporada.fechaInicio)} – ${fechaCorta(temporada.fechaFin)}`
}

/** «3 noches», «1 noche» o «Sin mínimo» (TEM-07). */
export function textoEstancia(noches: number): string {
  if (noches === 0) return 'Sin mínimo'
  return `${noches} ${noches === 1 ? 'noche' : 'noches'}`
}

/** Pesos escritos a mano («180.000», «$ 180000») como número; `NaN` si no son pesos enteros. */
export function leerPesos(texto: string): number {
  const limpio = texto.replace(/[$\s]/g, '')
  if (!/^\d{1,3}(\.\d{3})+$|^\d+$/.test(limpio)) return NaN
  return Number(limpio.replace(/\./g, ''))
}

/** «180.000»: el valor como se escribe en el campo, con el «$» afuera. */
export function escribirPesos(valor: number): string {
  return dinero(valor).replace(/^\$\s*/, '')
}

/** Mientras se escribe, los miles se separan solos: «180000» → «180.000». Lo que no es un número se deja como está. */
export function pesosAlEscribir(texto: string): string {
  const digitos = texto.replace(/[.\s]/g, '')
  return /^\d+$/.test(digitos) ? escribirPesos(Number(digitos)) : texto
}

/** El error del valor de una tarifa, o nulo si sirve (TAR-02). `obligatorio` falso deja pasar el vacío. */
export function errorDeValor(texto: string, obligatorio = true): string | null {
  if (!texto.trim()) return obligatorio ? 'Escribe el valor por persona y noche.' : null
  const valor = leerPesos(texto)
  if (Number.isNaN(valor)) return 'Escribe solo el número en pesos, sin decimales. Por ejemplo, 180.000.'
  if (valor <= 0) return 'Escribe un valor mayor que cero.'
  return null
}

/**
 * El error de unas fechas, o nulo si sirven: las dos, el fin no antes del inicio (TEM-01) y sin compartir ni un día con
 * otra temporada activa (TEM-02). `propia` es la temporada que se está cambiando, para no compararla consigo misma.
 */
export function errorDeFechas(nombre: string, inicio: string, fin: string, temporadas: Temporada[],
  propia?: string): string | null {
  if (!inicio || !fin) return 'Elige el primer y el último día de la temporada.'
  if (fin < inicio) return 'El fin de la temporada no puede ser anterior al inicio.'
  const otra = temporadas.find((t) => t.activa && !t.esBase && t.id !== propia && t.fechaInicio && t.fechaFin
    && inicio <= t.fechaFin && t.fechaInicio <= fin)
  if (!otra) return null
  const esta = nombre.trim() ? `La temporada ${nombre.trim()}` : 'Esta temporada'
  return `${esta} se solapa con ${otra.nombre} (${textoFechas(otra)}). No pueden compartir ni un día.`
}

/** Lo que pide «Nueva temporada»: los números como se escriben y la tarifa de cada apartamento por su código. */
export interface FormTemporada {
  nombre: string
  inicio: string
  fin: string
  estancia: string
  tarifas: Record<string, string>
}

export interface ErroresTemporada {
  nombre?: string
  fechas?: string
  estancia?: string
  tarifas: Record<string, string>
}

/** Lo que la pantalla sabe de cada apartamento para su tarifa: si está a la venta, la tarifa es obligatoria (TAR-03). */
export interface ApartamentoDeTarifa {
  codigo: string
  nombre: string
  activo: boolean
}

export function validarTemporada(form: FormTemporada, temporadas: Temporada[],
  apartamentos: ApartamentoDeTarifa[]): ErroresTemporada {
  const errores: ErroresTemporada = { tarifas: {} }
  if (!form.nombre.trim()) errores.nombre = 'Escribe el nombre de la temporada.'
  const fechas = errorDeFechas(form.nombre, form.inicio, form.fin, temporadas)
  if (fechas) errores.fechas = fechas
  if (!/^\d+$/.test(form.estancia.trim())) errores.estancia = 'Escribe cuántas noches como mínimo, o 0 si no hay mínimo.'
  for (const apartamento of apartamentos) {
    const error = errorDeValor(form.tarifas[apartamento.codigo] ?? '', apartamento.activo)
    if (error) {
      errores.tarifas[apartamento.codigo] = apartamento.activo && !form.tarifas[apartamento.codigo]?.trim()
        ? 'Está a la venta: necesita su tarifa en esta temporada.'
        : error
    }
  }
  return errores
}

export const hayErrores = (errores: ErroresTemporada) =>
  Boolean(errores.nombre || errores.fechas || errores.estancia || Object.keys(errores.tarifas).length > 0)

/** El cuerpo de `POST /api/temporadas`: solo las tarifas escritas. */
export function cuerpoTemporada(form: FormTemporada): NuevaTemporada {
  return {
    nombre: form.nombre.trim(),
    fechaInicio: form.inicio,
    fechaFin: form.fin,
    estanciaMinimaNoches: Number(form.estancia.trim()),
    tarifas: Object.entries(form.tarifas)
      .filter(([, texto]) => texto.trim())
      .map(([apartamento, texto]) => ({ apartamento, valorPorOcupante: leerPesos(texto) })),
  }
}

/**
 * Los rechazos de la API nombran las temporadas por su id («La temporada TEM-5 se solapa con TEM-2»): se cambian por
 * su nombre. Un id que el calendario no tiene es el de la temporada que se estaba creando.
 */
export function conNombres(mensaje: string, temporadas: Temporada[], nombreNueva = ''): string {
  return mensaje.replace(/TEM-\d+/g, (id) => temporadas.find((t) => t.id === id)?.nombre
    ?? (nombreNueva.trim() || 'la temporada nueva'))
}

/** La tarifa vigente de cada temporada, por su id. Si llegaran dos, la de versión mayor. */
export function vigentesPorTemporada(tarifas: Tarifa[]): Map<string, Tarifa> {
  const porTemporada = new Map<string, Tarifa>()
  for (const tarifa of tarifas) {
    const actual = porTemporada.get(tarifa.temporada)
    if (!actual || tarifa.version > actual.version) porTemporada.set(tarifa.temporada, tarifa)
  }
  return porTemporada
}
