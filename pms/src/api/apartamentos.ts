// Apartamentos del alojamiento, para el filtro del listado de reservas.
import { pedir } from './cliente'
import type { Pagina } from './reservas'

/**
 * Los códigos de todos los apartamentos: `GET /api/apartamentos` responde de 10 en 10, así que recorre las páginas.
 */
export async function listarCodigosDeApartamentos(): Promise<string[]> {
  const codigos: string[] = []
  let pagina = 0
  let totalPaginas = 1
  while (pagina < totalPaginas) {
    const respuesta = await pedir<Pagina<{ codigo: string }>>(`/api/apartamentos?pagina=${pagina}`)
    codigos.push(...respuesta.contenido.map((apartamento) => apartamento.codigo))
    totalPaginas = respuesta.totalPaginas
    pagina += 1
  }
  return codigos
}

/** Lo que usa el PMS de `ApartamentoResponse`. */
export interface Apartamento {
  codigo: string
  nombre: string
  /** Mascotas por reserva; `null` = las del alojamiento (DEC-93). */
  limiteMascotas: number | null
  /** PREPARADO, OCUPADO, PENDIENTE_PREPARACION, EN_PREPARACION o FUERA_DE_SERVICIO. Puede faltar en respuestas viejas. */
  estadoOperativo?: string
}

/** `GET /api/apartamentos/{codigo}` (público): su nombre, su límite de mascotas y si está preparado. */
export function consultarApartamento(codigo: string): Promise<Apartamento> {
  return pedir(`/api/apartamentos/${encodeURIComponent(codigo)}`)
}

/** `ApartamentoEncontradoResponse`: un apartamento disponible con su valor para esa estancia. */
export interface ApartamentoEncontrado {
  codigo: string
  nombre: string
  capacidad: number
  dormitorios: number
  imagenPrincipal: string | null
  caracteristicas: string[]
  valorEstimado: number
}

/**
 * `GET /api/apartamentos/disponibles` (RN-01 · RN-02 · RN-07).
 *
 * @param fechasNacimiento con ellas, el valor cuenta solo a los que pagan (RN-06)
 * @throws ErrorApi 400 si una fecha no tiene el formato esperado; 409 si la estancia es inválida
 */
export function buscarDisponibles({ entrada, salida, ocupantes, fechasNacimiento, pagina }: {
  entrada: string
  salida: string
  ocupantes: number
  fechasNacimiento: string[]
  pagina: number
}): Promise<Pagina<ApartamentoEncontrado>> {
  const consulta = new URLSearchParams({ entrada, salida, ocupantes: String(ocupantes), pagina: String(pagina) })
  fechasNacimiento.forEach((fecha) => consulta.append('fechasNacimiento', fecha))
  return pedir(`/api/apartamentos/disponibles?${consulta}`)
}

/** Lo que usa la preparación de `ApartamentoResponse`. */
export interface ApartamentoOperativo {
  codigo: string
  nombre: string
  estadoOperativo: string
  /** Por qué está fuera de servicio, si se dijo; solo mientras siga así (DEC-113). */
  motivoFueraDeServicio?: string | null
}

/** Límite de `DeclararFueraDeServicioRequest.motivo`. */
export const LARGO_MOTIVO_FUERA = 500

/**
 * `GET /api/apartamentos/pendientes-preparacion` (CU-41): los que están pendientes o en preparación, de 10 en 10.
 * Para el personal de servicio y el administrador.
 */
export function listarPendientesPreparacion(pagina: number): Promise<Pagina<ApartamentoOperativo>> {
  return pedir(`/api/apartamentos/pendientes-preparacion?pagina=${pagina}`)
}

/** `GET /api/apartamentos` (solo el personal, DEC-113): todos, recorriendo las páginas, para la vista del administrador. */
export async function listarTodosLosApartamentos(): Promise<ApartamentoOperativo[]> {
  const todos: ApartamentoOperativo[] = []
  let pagina = 0
  let totalPaginas = 1
  while (pagina < totalPaginas) {
    const respuesta = await pedir<Pagina<ApartamentoOperativo>>(`/api/apartamentos?pagina=${pagina}`)
    todos.push(...respuesta.contenido)
    totalPaginas = respuesta.totalPaginas
    pagina += 1
  }
  return todos
}

/** Cada paso de la preparación (EOPE-02 · EOPE-04). */
export type PasoPreparacion =
  | 'iniciar-preparacion'
  | 'marcar-preparado'
  | 'declarar-fuera-de-servicio'
  | 'marcar-pendiente-preparacion'

/**
 * `PUT /api/apartamentos/{codigo}/{paso}`. Iniciar y marcar preparado los puede hacer el personal de servicio;
 * declarar fuera de servicio, o devolverlo de ahí a pendiente, solo el administrador.
 *
 * @param motivo solo al declarar fuera de servicio, opcional (DEC-113)
 * @throws ErrorApi 409 si el estado ya cambió (otra persona lo movió o se ocupó); 403 si el rol no puede
 */
export function moverApartamento(codigo: string, paso: PasoPreparacion, motivo?: string): Promise<ApartamentoOperativo> {
  return pedir(`/api/apartamentos/${encodeURIComponent(codigo)}/${paso}`, {
    method: 'PUT',
    cuerpo: paso === 'declarar-fuera-de-servicio' && motivo ? { motivo } : undefined,
  })
}
