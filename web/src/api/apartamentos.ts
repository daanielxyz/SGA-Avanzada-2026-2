// Llamadas de la búsqueda y la cotización. Cada interface es el gemelo de un Response del backend: si el Response
// cambia en Java, se cambia aquí también (el contrato está en /swagger-ui.html).
import { pedir } from './cliente'

/** `ApartamentoPublicoResponse`: solo lo que ve el público de un apartamento. */
export interface ApartamentoPublico {
  codigo: string
  nombre: string
  descripcion: string | null
  capacidad: number
  dormitorios: number
  imagenes: { url: string; principal: boolean }[]
  caracteristicas: string[]
}

/** `CotizacionResponse`: el precio noche por noche y el total, en pesos sin decimales. */
export interface Cotizacion {
  desglose: {
    noche: string
    temporada: string
    tarifa: number
    ocupantesFacturables: number
    subtotal: number
  }[]
  total: number
}

/** Fechas en formato `AAAA-MM-DD`; la salida es el día en que se va: esa noche no se cobra. */
export interface Estadia {
  entrada: string
  salida: string
}

export function buscarDisponibles({ entrada, salida }: Estadia, ocupantes: number) {
  const consulta = new URLSearchParams({ entrada, salida, ocupantes: String(ocupantes) })
  return pedir<ApartamentoPublico[]>(`/api/portal/apartamentos/disponibles?${consulta}`)
}

/** La edad de cada ocupante a la fecha de entrada decide si paga (RN-06), por eso se piden las fechas de nacimiento. */
export function cotizar(apartamento: string, { entrada, salida }: Estadia, fechasNacimiento: string[]) {
  return pedir<Cotizacion>('/api/cotizaciones', {
    method: 'POST',
    cuerpo: { apartamento, entrada, salida, fechasNacimiento },
  })
}
