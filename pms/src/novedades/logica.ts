// Lo que la pantalla de Novedades calcula sin hablar con la API (pms/DISENO.md, «Sprint 5: novedades»).
import type { Atajo } from '../componentes/eleccion'
import type { EstadoNovedad, GravedadNovedad, Novedad } from '../api/novedades'
import { cantidad } from '../formato'
import { fechaYHora } from '../reserva/llegada/llegada'

export const MAXIMO_DESCRIPCION = 2000

export const NOMBRE_DEL_ESTADO: Record<EstadoNovedad, string> = {
  ABIERTA: 'Abierta',
  EN_REVISION: 'En revisión',
  CERRADA: 'Cerrada',
}

export const NOMBRE_DE_LA_GRAVEDAD: Record<GravedadNovedad, string> = {
  ALTA: 'Alta',
  MEDIA: 'Media',
  BAJA: 'Baja',
}

/** Las gravedades de la más a la menos urgente, con su «para qué» (el prompt de Claude Design). */
export const GRAVEDADES: readonly { gravedad: GravedadNovedad; explica: string }[] = [
  { gravedad: 'ALTA', explica: 'Impide usar algo importante o es un riesgo.' },
  { gravedad: 'MEDIA', explica: 'Molesta, pero el apartamento se puede usar.' },
  { gravedad: 'BAJA', explica: 'Un detalle o un faltante menor.' },
]

export const OPCIONES_ESTADO = Object.entries(NOMBRE_DEL_ESTADO) as [EstadoNovedad, string][]
export const OPCIONES_GRAVEDAD = GRAVEDADES.map(({ gravedad }) => [gravedad, NOMBRE_DE_LA_GRAVEDAD[gravedad]] as const)

/** Lo que el personal de servicio tiene pendiente: lo que se ve al entrar. */
export const POR_ATENDER: readonly EstadoNovedad[] = ['ABIERTA', 'EN_REVISION']
export const ATAJOS_ESTADO: readonly Atajo[] = [{ nombre: 'Por atender', valores: POR_ATENDER }]

/** El paso siguiente y cómo se dice en el botón; una cerrada no tiene (NOV-06). */
export function accionSiguiente(estado: EstadoNovedad): { texto: string; confirmar: string; enviando: string } | null {
  if (estado === 'ABIERTA') return { texto: 'Pasar a revisión', confirmar: 'Pasar a revisión', enviando: 'Pasando a revisión…' }
  if (estado === 'EN_REVISION') return { texto: 'Cerrar', confirmar: 'Cerrar novedad', enviando: 'Cerrando…' }
  return null
}

/** «hace 1 hora», «hace 2 días»: cuánto lleva la novedad desde que se reportó. */
export function hace(fechaHora: string, ahora: Date = new Date()): string {
  const minutos = Math.floor((ahora.getTime() - new Date(fechaHora).getTime()) / 60_000)
  if (minutos < 1) return 'hace un momento'
  if (minutos < 60) return `hace ${cantidad(minutos, 'minuto', 'minutos')}`
  if (minutos < 60 * 24) return `hace ${cantidad(Math.floor(minutos / 60), 'hora', 'horas')}`
  return `hace ${cantidad(Math.floor(minutos / (60 * 24)), 'día', 'días')}`
}

const SIN_NOMBRE = 'una persona que ya no está en el sistema'

/** «Reportada hace 1 hora por Luz Marina Cardona». */
export function reportada(novedad: Novedad, ahora: Date = new Date()): string {
  const primero = novedad.historial[0]
  if (!primero) return ''
  return `Reportada ${hace(primero.fechaHora, ahora)} por ${primero.autorNombre ?? SIN_NOMBRE}`
}

/** Un paso del historial dicho completo: «En revisión · 16 nov 2026, 8:30 a. m., por Andrés Felipe Gómez». */
export function textoPaso(paso: Novedad['historial'][number]): { estado: string; quien: string; cuando: string } {
  return {
    estado: NOMBRE_DEL_ESTADO[paso.estado] ?? paso.estado,
    quien: paso.autorNombre ?? 'Una persona que ya no está en el sistema',
    cuando: fechaYHora(paso.fechaHora),
  }
}

/** La descripción corta para un aviso: «APT-104 · Vidrio de la ventana del cuarto roto…». */
export function resumen(novedad: Pick<Novedad, 'apartamento' | 'descripcion'>, largo = 40): string {
  const texto = novedad.descripcion.trim()
  if (texto.length <= largo) return `${novedad.apartamento} · ${texto}`
  // Se corta en la última palabra entera, sin la coma o el punto que quede al final
  const cortado = texto.slice(0, largo)
  const espacio = cortado.lastIndexOf(' ')
  const corto = (espacio > largo / 2 ? cortado.slice(0, espacio) : cortado).replace(/[\s,.;:]+$/, '')
  return `${novedad.apartamento} · ${corto}…`
}

/** «ABIERTA,EN_REVISION» → los dos, sin vacíos ni repetidos. */
export function listaDe(valor: string | null): string[] {
  return [...new Set((valor ?? '').split(',').map((v) => v.trim()).filter(Boolean))]
}
