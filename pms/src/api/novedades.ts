// Novedades de mantenimiento: lo que el personal de servicio encuentra en un apartamento (CU-43 · CU-44 · CU-55).
import { pedir } from './cliente'
import type { Pagina } from './reservas'

export type EstadoNovedad = 'ABIERTA' | 'EN_REVISION' | 'CERRADA'
export type GravedadNovedad = 'ALTA' | 'MEDIA' | 'BAJA'

/** Un paso del historial: quién lo dio y cuándo. */
export interface PasoNovedad {
  estado: EstadoNovedad
  autor: string
  /** Su nombre vigente; nulo si esa cuenta ya no existe (DEC-132). */
  autorNombre: string | null
  fechaHora: string
}

/** `NovedadResponse`. */
export interface Novedad {
  id: string
  apartamento: string
  descripcion: string
  gravedad: GravedadNovedad
  /** El del último paso. */
  estado: EstadoNovedad
  /** Del registro al paso más reciente. */
  historial: PasoNovedad[]
}

/** Los filtros de la lista general: cada uno con varios valores o vacío («cualquiera»). */
export interface FiltroNovedades {
  estados: string[]
  gravedades: string[]
  apartamentos: string[]
}

/**
 * `GET /api/novedades` (DEC-132): las de todos los apartamentos, de la más reciente a la más antigua, de 10 en 10.
 * Cada filtro va con sus valores separados por comas, como en Reservas (DEC-119).
 */
export function listarNovedades(filtro: FiltroNovedades, pagina: number): Promise<Pagina<Novedad>> {
  const consulta = new URLSearchParams()
  if (filtro.estados.length) consulta.set('estado', filtro.estados.join(','))
  if (filtro.gravedades.length) consulta.set('gravedad', filtro.gravedades.join(','))
  if (filtro.apartamentos.length) consulta.set('apartamento', filtro.apartamentos.join(','))
  consulta.set('pagina', String(pagina))
  return pedir<Pagina<Novedad>>(`/api/novedades?${consulta}`)
}

/**
 * `POST /api/apartamentos/{codigo}/novedades`: nace abierta. No bloquea el apartamento ni cambia su preparación
 * (NOV-03).
 *
 * @throws ErrorApi 404 si el apartamento no existe; 400 sin descripción o con una gravedad que no existe
 */
export function reportarNovedad(apartamento: string, descripcion: string, gravedad: GravedadNovedad): Promise<Novedad> {
  return pedir<Novedad>(`/api/apartamentos/${encodeURIComponent(apartamento)}/novedades`, {
    method: 'POST',
    cuerpo: { descripcion, gravedad },
  })
}

/**
 * `PUT /api/novedades/{id}/avanzar`: ABIERTA → EN_REVISION → CERRADA, con quién y cuándo.
 *
 * @throws ErrorApi 409 si ya está cerrada (otra persona pudo moverla); 404 si no existe
 */
export function avanzarNovedad(id: string): Promise<Novedad> {
  return pedir<Novedad>(`/api/novedades/${encodeURIComponent(id)}/avanzar`, { method: 'PUT' })
}
