// Textos y cuentas de «Canales de venta» (pms/DISENO.md, «Sprint 5: canales de venta»). Nunca se muestra un valor
// crudo de la API (EXTERNO, CREAR_RESERVA): siempre su texto.
import type { Canal, Conflicto, OperacionCanal, ResultadoEvento, TipoCanal } from '../api/canales'
import { cantidad, fechaCorta, fechaHoy, fechaSinAnio, hora, noches, sumarDias } from '../formato'

export const MAXIMO_NOMBRE = 80
export const MAXIMO_REFERENCIA = 120
export const MAXIMO_DECISION = 500

export const TIPOS: readonly { tipo: TipoCanal; nombre: string; explica: string }[] = [
  { tipo: 'PORTAL', nombre: 'Portal web', explica: 'La web de reservas del hotel.' },
  { tipo: 'DIRECTO', nombre: 'Directo', explica: 'Las reservas que crea recepción: por teléfono, en el mostrador o por WhatsApp.' },
  { tipo: 'EXTERNO', nombre: 'Externo', explica: 'Una plataforma como Booking o Airbnb, que se conecta sola al sistema con una clave.' },
]

export const NOMBRE_DEL_TIPO: Record<TipoCanal, string> = { PORTAL: 'Portal web', DIRECTO: 'Directo', EXTERNO: 'Externo' }

export const TEXTO_OPERACION: Record<OperacionCanal, string> = {
  CONSULTAR_DISPONIBILIDAD: 'Consultó disponibilidad',
  CREAR_RESERVA: 'Envió una reserva',
  CANCELAR_RESERVA: 'Canceló una reserva',
  PUBLICAR_DISPONIBILIDAD: 'Le enviamos la disponibilidad',
}

export const RESULTADOS: Record<ResultadoEvento, { texto: string; clase: string }> = {
  EXITOSO: { texto: 'Hecho', clase: 'resultado-hecho' },
  DUPLICADO: { texto: 'Repetido', clase: 'resultado-repetido' },
  CONFLICTO: { texto: 'Rechazado por disponibilidad', clase: 'resultado-disponibilidad' },
  RECHAZADO: { texto: 'Rechazado', clase: 'resultado-rechazado' },
  ERROR: { texto: 'Error', clase: 'resultado-error' },
}

/**
 * Solo un canal externo se puede desactivar desde aquí: el backend revisa si está activo únicamente cuando entra algo
 * de un canal externo, así que desactivar el portal o recepción no cambiaría nada.
 */
export const sePuedeDesactivar = (canal: Canal) => canal.activo && canal.tipo === 'EXTERNO'

/** Solo los externos hablan con el sistema y dejan bitácora. */
export const tieneBitacora = (canal: Canal) => canal.tipo === 'EXTERNO'

/** «20 nov 2026 → 23 nov 2026 · 3 noches»: la noche de salida no se cuenta (EST-01). */
export function estancia(conflicto: Pick<Conflicto, 'entrada' | 'salida'>): string {
  const total = noches(conflicto.entrada, conflicto.salida)
  return `${fechaCorta(conflicto.entrada)} → ${fechaCorta(conflicto.salida)} · ${cantidad(total, 'noche', 'noches')}`
}

/** «9:12 p. m.» de una fecha y hora de la API (AAAA-MM-DDTHH:MM…), sin que la zona del navegador la corra. */
export const horaDe = (fechaHora: string) => hora(fechaHora.slice(11, 16))

/** «hace 13 horas · 16 nov, 9:12 p. m.»: cuánto lleva esperando y cuándo exactamente. */
export function detectado(fechaHora: string, ahora: Date = new Date()): string {
  const minutos = Math.floor((ahora.getTime() - new Date(fechaHora).getTime()) / 60_000)
  const hace = minutos < 1 ? 'hace un momento'
    : minutos < 60 ? `hace ${cantidad(minutos, 'minuto', 'minutos')}`
      : minutos < 60 * 24 ? `hace ${cantidad(Math.floor(minutos / 60), 'hora', 'horas')}`
        : `hace ${cantidad(Math.floor(minutos / (60 * 24)), 'día', 'días')}`
  return `${hace} · ${fechaSinAnio(fechaHora)}, ${horaDe(fechaHora)}`
}

/** El enlace para llamar: solo los dígitos y el «+» del principio. */
export function enlaceTelefono(telefono: string): string {
  return `tel:${telefono.trim().replace(/(?!^\+)\D/g, '')}`
}

/** La bitácora abre con los últimos 7 días, los dos extremos incluidos. */
export function rangoInicial(ahora: Date = new Date()): { desde: string; hasta: string } {
  const hasta = fechaHoy(ahora)
  return { desde: sumarDias(hasta, -6), hasta }
}

/** El error del filtro de fechas, o null si se puede consultar. */
export function errorDeRango(desde: string, hasta: string): { campo: 'desde' | 'hasta'; mensaje: string } | null {
  if (!desde) return { campo: 'desde', mensaje: 'Elige desde qué día.' }
  if (!hasta) return { campo: 'hasta', mensaje: 'Elige hasta qué día.' }
  if (hasta < desde) return { campo: 'hasta', mensaje: '«Hasta» no puede ser antes de «Desde».' }
  return null
}

/** El mensaje del canal legible: si es JSON, con sangría; si no, tal cual. */
export function mensajeLegible(cargaUtil: string): string {
  try {
    return JSON.stringify(JSON.parse(cargaUtil), null, 2)
  } catch {
    return cargaUtil
  }
}

/** Parte un texto en trozos normales y códigos del sistema (APT-103, RES-2026-00042, BK-558120). */
export function partirCodigos(texto: string): { texto: string; codigo: boolean }[] {
  return texto.split(/\b([A-Z]{2,4}-\d{4}-\d{3,}|[A-Z]{2,4}-\d+)\b/).filter(Boolean)
    .map((parte) => ({ texto: parte, codigo: /^[A-Z]{2,4}-\d/.test(parte) }))
}
