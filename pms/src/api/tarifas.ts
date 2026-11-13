// Temporadas del alojamiento y tarifas de cada apartamento (sprint 4, pms/DISENO.md «Sprint 4: temporadas y tarifas»).
import { pedir } from './cliente'
import type { Pagina } from './reservas'

/** `TemporadaResponse`. La base no tiene fechas ni estancia mínima y siempre está activa (TEM-09 · TEM-10). */
export interface Temporada {
  /** `TEM-n`: nunca se muestra. */
  id: string
  nombre: string
  /** Incluida, como `fechaFin` (TEM-01). `null` en la base. */
  fechaInicio: string | null
  fechaFin: string | null
  esBase: boolean
  /** 0 = sin mínimo. */
  estanciaMinimaNoches: number
  activa: boolean
}

/** `TarifaResponse`: una versión del valor por ocupante facturable y noche (TAR-01 · TAR-05). */
export interface Tarifa {
  id: string
  apartamento: string
  /** El id de la temporada (`TEM-n`): el nombre sale del calendario. */
  temporada: string
  valorPorOcupante: number
  /** Se cuenta por apartamento y temporada. */
  version: number
  vigenteDesde: string
}

export interface NuevaTemporada {
  nombre: string
  fechaInicio: string
  fechaFin: string
  estanciaMinimaNoches: number
  /** Obligatoria la de cada apartamento a la venta (TAR-03); la de los retirados, opcional. */
  tarifas: { apartamento: string; valorPorOcupante: number }[]
}

/** `GET /api/temporadas`: todas, también las inactivas. */
export async function consultarTemporadas(): Promise<Temporada[]> {
  return (await pedir<{ temporadas: Temporada[] }>('/api/temporadas')).temporadas
}

/**
 * `POST /api/temporadas` (CU-33).
 *
 * @throws ErrorApi 409 si se solapa con otra activa (TEM-02) o le falta la tarifa de un apartamento a la venta (TAR-03)
 */
export function agregarTemporada(nueva: NuevaTemporada): Promise<{ temporada: Temporada; tarifas: Tarifa[] }> {
  return pedir('/api/temporadas', { method: 'POST', cuerpo: nueva })
}

/** `PUT /api/temporadas/{id}/fechas` (TEM-05): devuelve el calendario entero. @throws ErrorApi 409 si se solapa */
export async function cambiarFechasTemporada(id: string, fechaInicio: string, fechaFin: string): Promise<Temporada[]> {
  return (await pedir<{ temporadas: Temporada[] }>(`/api/temporadas/${encodeURIComponent(id)}/fechas`, {
    method: 'PUT',
    cuerpo: { fechaInicio, fechaFin },
  })).temporadas
}

/** `PUT /api/temporadas/{id}/desactivar` (TEM-06): no se vuelve a activar. Devuelve el calendario entero. */
export async function desactivarTemporada(id: string): Promise<Temporada[]> {
  return (await pedir<{ temporadas: Temporada[] }>(`/api/temporadas/${encodeURIComponent(id)}/desactivar`, {
    method: 'PUT',
  })).temporadas
}

const rutaTarifas = (codigo: string) => `/api/apartamentos/${encodeURIComponent(codigo)}/tarifas`

/**
 * `GET /api/apartamentos/{codigo}/tarifas`: las vigentes hoy, una por temporada (TAR-06). Pueden venir también las de
 * temporadas inactivas. Responde de 10 en 10: recorre las páginas.
 */
export async function tarifasVigentes(codigo: string): Promise<Tarifa[]> {
  const tarifas: Tarifa[] = []
  for (let pagina = 0, total = 1; pagina < total; pagina += 1) {
    const respuesta = await pedir<Pagina<Tarifa>>(`${rutaTarifas(codigo)}?pagina=${pagina}`)
    tarifas.push(...respuesta.contenido)
    total = respuesta.totalPaginas
  }
  return tarifas
}

/**
 * `PUT /api/apartamentos/{codigo}/tarifas/{temporada}` (CU-34): crea la versión siguiente, que rige desde hoy.
 *
 * @throws ErrorApi 409 si la temporada está inactiva
 */
export function definirTarifa(codigo: string, temporada: string, valorPorOcupante: number): Promise<Tarifa> {
  return pedir(`${rutaTarifas(codigo)}/${encodeURIComponent(temporada)}`, { method: 'PUT', cuerpo: { valorPorOcupante } })
}

/** `GET …/tarifas/historial`: todas las versiones, de la más reciente a la más antigua (TAR-05). */
export function historialTarifas(codigo: string, pagina: number): Promise<Pagina<Tarifa>> {
  return pedir(`${rutaTarifas(codigo)}/historial?pagina=${pagina}`)
}
