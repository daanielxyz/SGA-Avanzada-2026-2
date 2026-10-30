// Llamadas de la búsqueda. Cada interface es el gemelo de un Response del backend: si el Response cambia en Java, se
// cambia aquí también (el contrato está en /swagger-ui.html).
import { pedir } from './cliente'

/** `PrecioResponse`: lo que cuesta la estancia buscada para el grupo, noche por noche, en pesos sin decimales. */
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
  /** Viene en la búsqueda; en la ficha, sin fechas, va vacío. */
  precio: Precio | null
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

/** Todos cuentan para la capacidad (RN-02); solo los adultos pagan. */
export function buscarDisponibles({ entrada, salida }: Estadia, { adultos, ninos }: Grupo) {
  const consulta = new URLSearchParams({ entrada, salida, ocupantes: String(adultos + ninos), ninos: String(ninos) })
  return pedir<ApartamentoPublico[]>(`/api/portal/apartamentos/disponibles?${consulta}`)
}
