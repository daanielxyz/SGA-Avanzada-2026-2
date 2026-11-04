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

/** `GET /api/apartamentos/{codigo}` (público): su nombre, que la reserva no trae. */
export function consultarApartamento(codigo: string): Promise<{ codigo: string; nombre: string }> {
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
