// El envío del TRA de cada reserva al Ministerio de Comercio (DEC-130): recepción y administración.
import { ErrorApi, pedir } from './cliente'
import type { Pagina } from './reservas'

export type EstadoEnvioTra = 'PENDIENTE' | 'ENVIADO' | 'CON_ERROR'

/** `EnvioTraResponse`: en qué va, sin los datos personales enviados. */
export interface EnvioTra {
  reserva: string
  estado: EstadoEnvioTra
  /** El que devolvió el Ministerio para el huésped principal; null si aún no entró. */
  codigoMinisterio: string | null
  acompanantesEnviados: number
  intentos: number
  ultimoIntento: string | null
  /** Por qué falló el último intento; solo `CON_ERROR`. */
  ultimoError: string | null
  enviadoEn: string | null
}

/** `GET /api/reservas/{codigo}/tra`. Null si todavía no hay envío: el grupo no ha llegado (404). */
export async function consultarEnvioTra(codigo: string): Promise<EnvioTra | null> {
  try {
    return await pedir<EnvioTra>(`/api/reservas/${encodeURIComponent(codigo)}/tra`)
  } catch (error) {
    if (error instanceof ErrorApi && error.estado === 404) return null
    throw error
  }
}

/**
 * `PUT /api/reservas/{codigo}/tra/enviar`: «Reintentar». Responde también cuando el Ministerio no lo recibió (queda
 * `CON_ERROR` con el motivo); 409 si ya se envió o faltan datos.
 */
export function enviarTra(codigo: string): Promise<EnvioTra> {
  return pedir(`/api/reservas/${encodeURIComponent(codigo)}/tra/enviar`, { method: 'PUT' })
}

/** `GET /api/tra/envios?estado=&pagina=`: del intento más reciente al más antiguo, de 10 en 10. */
export function listarEnviosTra(estado: EstadoEnvioTra, pagina = 0): Promise<Pagina<EnvioTra>> {
  return pedir(`/api/tra/envios?estado=${estado}&pagina=${pagina}`)
}
