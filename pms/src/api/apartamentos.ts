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
}

/**
 * `GET /api/apartamentos/pendientes-preparacion` (CU-41): los que están pendientes o en preparación, de 10 en 10.
 * Para el personal de servicio y el administrador.
 */
export function listarPendientesPreparacion(pagina: number): Promise<Pagina<ApartamentoOperativo>> {
  return pedir(`/api/apartamentos/pendientes-preparacion?pagina=${pagina}`)
}

/** `GET /api/apartamentos` (público): todos, recorriendo las páginas, para la vista del administrador. */
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
 * @throws ErrorApi 409 si el estado ya cambió (otra persona lo movió o se ocupó); 403 si el rol no puede
 */
export function moverApartamento(codigo: string, paso: PasoPreparacion): Promise<ApartamentoOperativo> {
  return pedir(`/api/apartamentos/${encodeURIComponent(codigo)}/${paso}`, { method: 'PUT' })
}

// ── Configuración de apartamentos (sprint 4): solo el administrador ──

/** `ImagenResponse`: la dirección de una foto ya publicada; exactamente una es la principal (IMG-02). */
export interface Imagen {
  url: string
  principal: boolean
}

/** `BloqueoResponse`. `fechaFin` es exclusiva, como la salida de una reserva; `vigente` = no se ha levantado. */
export interface Bloqueo {
  id: string
  fechaInicio: string
  fechaFin: string
  motivo: string
  vigente: boolean
}

/** `ApartamentoResponse` completo, como lo ve el administrador. */
export interface FichaApartamento {
  codigo: string
  nombre: string
  descripcion: string | null
  capacidad: number
  dormitorios: number
  estadoOperativo: string
  /** A la venta; nace retirado. */
  activo: boolean
  imagenes: Imagen[]
  caracteristicas: string[]
  bloqueos: Bloqueo[]
  /** Mascotas por reserva; `null` = el límite del alojamiento (DEC-93). */
  limiteMascotas: number | null
  /** Solo mientras está fuera de servicio (DEC-113). */
  motivoFueraDeServicio?: string | null
}

/** Lo que se envía al crear (`CrearApartamentoRequest`). */
export interface NuevoApartamento {
  codigo: string
  nombre: string
  descripcion: string
  capacidad: number
  dormitorios: number
  imagenes: Imagen[]
  caracteristicas: string[]
}

/** Lo que se cambia en «Datos» (`ActualizarApartamentoRequest`): el código y la capacidad no. */
export type DatosApartamento = Pick<NuevoApartamento, 'nombre' | 'descripcion' | 'dormitorios' | 'caracteristicas'>

const ruta = (codigo: string, accion = '') => `/api/apartamentos/${encodeURIComponent(codigo)}${accion}`

/** `GET /api/apartamentos?pagina=` (DEC-18): todos, a la venta y retirados, de 10 en 10. */
export function listarApartamentos(pagina: number): Promise<Pagina<FichaApartamento>> {
  return pedir(`/api/apartamentos?pagina=${pagina}`)
}

/** `GET /api/apartamentos/{codigo}`. @throws ErrorApi 404 si no existe */
export function consultarFicha(codigo: string): Promise<FichaApartamento> {
  return pedir(ruta(codigo))
}

/** `POST /api/apartamentos` (IMG-01 · IMG-02 · CARAC-01). @throws ErrorApi 409 `DUPLICADO` si el código ya existe */
export function crearApartamento(nuevo: NuevoApartamento): Promise<FichaApartamento> {
  return pedir('/api/apartamentos', { method: 'POST', cuerpo: nuevo })
}

/** `PUT /api/apartamentos/{codigo}`: nombre, descripción, dormitorios y características. */
export function actualizarApartamento(codigo: string, datos: DatosApartamento): Promise<FichaApartamento> {
  return pedir(ruta(codigo), { method: 'PUT', cuerpo: datos })
}

/** `CambioCapacidadResponse`: el apartamento y las reservas que ya no caben, para revisarlas con recepción. */
export interface CambioCapacidad {
  apartamento: FichaApartamento
  reservasAfectadas: string[]
}

/** `PUT …/capacidad`: no rechaza por las reservas que ya no caben, las devuelve. */
export function cambiarCapacidad(codigo: string, capacidad: number): Promise<CambioCapacidad> {
  return pedir(ruta(codigo, '/capacidad'), { method: 'PUT', cuerpo: { capacidad } })
}

/**
 * Las fotos, por su dirección: agregar (la primera queda principal), quitar (si era la principal, la toma la primera de
 * las que quedan) o hacer principal (IMG-01 · IMG-02).
 *
 * @throws ErrorApi 409 con más de 10, una dirección repetida o la última de un apartamento a la venta
 */
export function cambiarImagen(codigo: string, accion: 'agregar' | 'quitar' | 'principal', url: string):
Promise<FichaApartamento> {
  return accion === 'agregar'
    ? pedir(ruta(codigo, '/imagenes'), { method: 'POST', cuerpo: { url } })
    : pedir(ruta(codigo, `/imagenes/${accion}`), { method: 'PUT', cuerpo: { url } })
}

/** `PUT …/limite-mascotas` (RP-03): desde 0. */
export function fijarLimiteMascotas(codigo: string, limiteMascotas: number): Promise<FichaApartamento> {
  return pedir(ruta(codigo, '/limite-mascotas'), { method: 'PUT', cuerpo: { limiteMascotas } })
}

/** `POST …/bloqueos`: esas noches no se venden en ningún canal. `fin` es exclusiva. */
export function registrarBloqueo(codigo: string, bloqueo: { inicio: string; fin: string; motivo: string }):
Promise<FichaApartamento> {
  return pedir(ruta(codigo, '/bloqueos'), { method: 'POST', cuerpo: bloqueo })
}

/** `PUT …/bloqueos/{id}/levantar`: esas noches vuelven a venderse. */
export function levantarBloqueo(codigo: string, id: string): Promise<FichaApartamento> {
  return pedir(ruta(codigo, `/bloqueos/${encodeURIComponent(id)}/levantar`), { method: 'PUT' })
}

/**
 * `PUT …/activar` o `…/retirar`. Activar pide al menos una foto y tarifa en todas las temporadas (APA-11 · TAR-03);
 * retirar, que no tenga reservas activas o futuras.
 */
export function cambiarVenta(codigo: string, accion: 'activar' | 'retirar'): Promise<FichaApartamento> {
  return pedir(ruta(codigo, `/${accion}`), { method: 'PUT' })
}
