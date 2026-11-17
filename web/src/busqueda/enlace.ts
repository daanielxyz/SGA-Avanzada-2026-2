import type { ApartamentoPublico, Estadia, Grupo } from '../api/apartamentos'
import { masDias } from '../formato'

/** La página del apartamento abre con lo que se ve en la tarjeta: las fechas de la búsqueda o la noche del precio. */
export function enlaceAlApartamento({ codigo, precio }: ApartamentoPublico, busqueda?: { estadia: Estadia; grupo: Grupo }) {
  const noche = precio?.desglose[0]?.noche
  const parametros = busqueda
    ? new URLSearchParams({ ...busqueda.estadia, adultos: String(busqueda.grupo.adultos),
      ninos: String(busqueda.grupo.ninos) })
    : new URLSearchParams(noche ? { entrada: noche, salida: masDias(noche, 1) } : {})
  const consulta = parametros.toString()
  return `/apartamentos/${encodeURIComponent(codigo)}${consulta ? `?${consulta}` : ''}`
}
