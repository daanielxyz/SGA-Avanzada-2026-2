// Llamadas de los apartamentos del portal. Cada interface es el gemelo de un Response del backend: si el Response
// cambia en Java, se cambia aquí también (el contrato está en /swagger-ui.html).
import { pedir } from './cliente'

/** `PrecioResponse`: lo que cuesta una estancia, noche por noche, en pesos sin decimales. */
export interface Precio {
  desglose: {
    noche: string
    /** Lo que paga cada adulto esa noche, según su temporada. */
    tarifa: number
    ocupantesFacturables: number
    subtotal: number
  }[]
  total: number
}

/** `ApartamentoPublicoResponse`: solo lo que ve el público de un apartamento. */
export interface ApartamentoPublico {
  codigo: string
  nombre: string
  descripcion: string | null
  capacidad: number
  dormitorios: number
  imagenes: { url: string; principal: boolean }[]
  caracteristicas: string[]
  /**
   * En el listado y la ficha, lo que paga un adulto su próxima noche libre; en la búsqueda por fechas, la estancia
   * del grupo. Vacío si no tiene noches libres en el año.
   */
  precio: Precio | null
  /** En el listado y la ficha, la primera noche que se puede reservar desde hoy; vacía en la búsqueda por fechas. */
  proximaNocheLibre: string | null
}

/** `EstanciaPortalResponse`: si unas fechas y un grupo caben en un apartamento y cuánto costaría. */
export interface EstanciaPortal {
  disponible: boolean
  precio: Precio
  /** La primera noche libre desde la llegada pedida (la misma llegada si está libre). */
  proximaNocheLibre: string | null
}

/** Fechas en formato `AAAA-MM-DD`; la salida es el día en que se va: esa noche no se cobra. */
export interface Estadia {
  entrada: string
  salida: string
}

/** Quiénes viajan: los adultos alcanzan la edad desde la que se paga el día de llegada (RN-06); los niños no. */
export interface Grupo {
  adultos: number
  ninos: number
}

// Todos cuentan para la capacidad (RN-02); solo los adultos pagan
function consulta({ entrada, salida }: Estadia, { adultos, ninos }: Grupo) {
  return new URLSearchParams({ entrada, salida, ocupantes: String(adultos + ninos), ninos: String(ninos) })
}

export function listarApartamentos() {
  return pedir<ApartamentoPublico[]>('/api/portal/apartamentos')
}

export function buscarDisponibles(estadia: Estadia, grupo: Grupo) {
  return pedir<ApartamentoPublico[]>(`/api/portal/apartamentos/disponibles?${consulta(estadia, grupo)}`)
}

export function consultarApartamento(codigo: string) {
  return pedir<ApartamentoPublico>(`/api/portal/apartamentos/${encodeURIComponent(codigo)}`)
}

export function consultarEstancia(codigo: string, estadia: Estadia, grupo: Grupo) {
  return pedir<EstanciaPortal>(
    `/api/portal/apartamentos/${encodeURIComponent(codigo)}/estancia?${consulta(estadia, grupo)}`)
}
