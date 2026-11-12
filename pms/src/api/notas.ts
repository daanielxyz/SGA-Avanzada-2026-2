// Notas de la reserva para el siguiente turno (BL-08 · DEC-105). No se editan ni se borran: se marcan como atendidas.
// Las tres rutas responden la lista completa, de la más reciente a la más antigua.
import { pedir } from './cliente'

export interface Nota {
  id: string
  texto: string
  autor: string
  /** Nulo si esa persona ya no existe. */
  autorNombre: string | null
  escritaEn: string
  atendida: boolean
  atencion: { autor: string; autorNombre: string | null; fechaHora: string } | null
}

/** Límite de `EscribirNotaRequest.texto`. */
export const NOTA_MAXIMA = 500

function ruta(codigo: string, resto = ''): string {
  return `/api/reservas/${encodeURIComponent(codigo)}/notas${resto}`
}

/** `GET /api/reservas/{codigo}/notas`. */
export function listarNotas(codigo: string): Promise<Nota[]> {
  return pedir<Nota[]>(ruta(codigo))
}

/**
 * `POST /api/reservas/{codigo}/notas`.
 *
 * @throws ErrorApi 400 si el texto está vacío o pasa de 500; 409 `ESTADO_NO_PERMITIDO` si la reserva ya terminó
 */
export function escribirNota(codigo: string, texto: string): Promise<Nota[]> {
  return pedir<Nota[]>(ruta(codigo), { method: 'POST', cuerpo: { texto } })
}

/**
 * `PUT /api/reservas/{codigo}/notas/{id}/atender`.
 *
 * @throws ErrorApi 409 `YA_ATENDIDA` si otro turno ya lo hizo, `ESTADO_NO_PERMITIDO` si la reserva terminó
 */
export function atenderNota(codigo: string, id: string): Promise<Nota[]> {
  return pedir<Nota[]>(ruta(codigo, `/${encodeURIComponent(id)}/atender`), { method: 'PUT' })
}
