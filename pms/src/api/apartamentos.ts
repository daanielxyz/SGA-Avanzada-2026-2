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
