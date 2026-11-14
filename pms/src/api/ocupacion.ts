// Tablero de ocupación de recepción (DEC-86): qué tiene cada apartamento en un rango de noches.
import { pedir } from './cliente'
import type { SituacionSaldo } from './folio'
import type { CanalOrigen, EstadoReserva, Pagina } from './reservas'

export interface ReservaOcupada {
  codigo: string
  entrada: string
  /** Exclusiva: esa noche no se ocupa (EST-01). */
  salida: string
  estado: EstadoReserva
  canalOrigen: CanalOrigen
  titularId: string
  /** DEC-101: el titular y la cuenta. Pueden faltar en respuestas de antes. */
  titularNombre?: string | null
  saldo?: number
  situacionSaldo?: SituacionSaldo
  totalPagado?: number
}

export interface BloqueoOcupado {
  id: string
  fechaInicio: string
  /** Exclusiva, como la salida de una estancia (BLO-02). */
  fechaFin: string
  motivo: string | null
}

/**
 * `OcupacionResponse`: llegan las reservas activas, las finalizadas y las no-show que tocan el rango (DEC-101), y los
 * bloqueos vigentes. Las canceladas no.
 */
export interface OcupacionApartamento {
  apartamento: string
  nombre: string
  estadoOperativo: string
  /** Por qué está fuera de servicio, si se dijo (DEC-113). */
  motivoFueraDeServicio?: string | null
  reservas: ReservaOcupada[]
  bloqueos: BloqueoOcupado[]
}

/**
 * `GET /api/ocupacion`.
 *
 * @param hasta exclusiva: la primera noche que ya no se muestra
 * @throws ErrorApi 409 si el rango es inválido
 */
export function consultarOcupacion({ desde, hasta, apartamento, pagina }: {
  desde: string
  hasta: string
  apartamento: string
  pagina: number
}): Promise<Pagina<OcupacionApartamento>> {
  const consulta = new URLSearchParams({ desde, hasta, pagina: String(pagina) })
  if (apartamento) consulta.set('apartamento', apartamento)
  return pedir(`/api/ocupacion?${consulta}`)
}
