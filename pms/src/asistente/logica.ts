// Lo que el panel de ayuda calcula sin pantalla (DEC-127): el historial que se envía, a dónde lleva cada fuente, el
// texto de la respuesta en bloques y las preguntas sugeridas de cada rol.
import type { MensajeAnterior, RespuestaAsistente } from '../api/asistente'
import type { Rol } from '../sesion/roles'

/** Lo más largo que acepta el backend (`PreguntaRequest.pregunta`). */
export const LARGO_MAXIMO = 1000
/** Desde aquí se muestra el contador. */
export const LARGO_CONTADOR = 900
/** El backend acepta hasta 20 mensajes anteriores y usa los 10 más recientes. */
const HISTORIAL_MAXIMO = 10

export interface Intercambio {
  id: number
  pregunta: string
  estado: 'esperando' | 'respondida' | 'fallida'
  respuesta?: RespuestaAsistente
  /** Si falló: por qué, para mostrarlo bajo la pregunta. */
  error?: string
}

export const SUGERENCIAS: Record<Rol, readonly string[]> = {
  RECEPCIONISTA: ['¿Cómo registro una llegada?', '¿Qué hago si el huésped no llegó?', '¿Cómo corrijo un pago mal registrado?'],
  PERSONAL_SERVICIO: [
    '¿Por cuál apartamento empiezo?',
    'Encontré un daño, ¿qué hago?',
    '¿Cómo marco un apartamento como preparado?',
  ],
  ADMINISTRADOR: [
    '¿Cómo pongo un apartamento a la venta?',
    '¿Cómo subo los precios de una temporada?',
    '¿Cómo autorizo una salida con saldo?',
  ],
}

/** Lo que el error de validación del campo dice, o nulo si se puede enviar. */
export function errorDePregunta(texto: string): string | null {
  if (!texto.trim()) return 'Escribe tu pregunta.'
  if (texto.length > LARGO_MAXIMO) return 'Tu pregunta pasa de 1.000 caracteres. Acórtala para enviarla.'
  return null
}

/** Los intercambios ya respondidos, como los espera el backend: pregunta y respuesta, los más recientes. */
export function historialPara(intercambios: readonly Intercambio[]): MensajeAnterior[] {
  const mensajes = intercambios
    .filter((i) => i.estado === 'respondida' && i.respuesta)
    .flatMap((i): MensajeAnterior[] => [
      { autor: 'PERSONA', texto: i.pregunta },
      { autor: 'ASISTENTE', texto: (i.respuesta as RespuestaAsistente).respuesta },
    ])
  return mensajes.slice(-HISTORIAL_MAXIMO)
}

/**
 * A dónde lleva una fuente. «/reservas/{codigo}/folio» toma el código de la pantalla actual si es de ese tipo
 * («/reservas/RES-2026-00042/llegada» → «/reservas/RES-2026-00042/folio»); si no, la fuente no lleva a ningún lado.
 */
export function rutaDeFuente(pantalla: string, rutaActual: string): string | null {
  if (!pantalla.startsWith('/')) return null
  const posicion = pantalla.indexOf('{codigo}')
  if (posicion < 0) return pantalla
  const prefijo = pantalla.slice(0, posicion)
  if (!rutaActual.startsWith(prefijo)) return null
  const codigo = rutaActual.slice(prefijo.length).split('/')[0]
  // «/reservas/nueva» no es una reserva
  if (!codigo || codigo === 'nueva') return null
  return pantalla.replace('{codigo}', codigo)
}

/** La pregunta que manda el «¿Por qué?» de un rechazo. */
export function preguntaDelRechazo(asunto: string, mensaje: string): string {
  const dice = `${asunto}: ${mensaje}`.replace(/\s+/g, ' ').trim().replace(/[.:]$/, '')
  return `¿Por qué el sistema dice: «${dice}.»?`
}

export type Bloque =
  | { tipo: 'parrafo'; texto: string }
  | { tipo: 'numerada' | 'vinetas'; elementos: string[] }

const NUMERADO = /^\s*\d+[.)]\s+/
const VINETA = /^\s*[-*•]\s+/

/**
 * La respuesta en bloques: párrafos, listas numeradas y listas con viñetas. No se interpreta nada más del Markdown
 * (DEC-127): el texto se pinta como texto, nunca como HTML.
 */
export function bloques(texto: string): Bloque[] {
  const resultado: Bloque[] = []
  let parrafo: string[] = []
  const cerrarParrafo = () => {
    if (parrafo.length > 0) resultado.push({ tipo: 'parrafo', texto: parrafo.join(' ') })
    parrafo = []
  }
  for (const linea of texto.replace(/\r\n/g, '\n').split('\n')) {
    const lista = NUMERADO.test(linea) ? 'numerada' : VINETA.test(linea) ? 'vinetas' : null
    if (lista) {
      cerrarParrafo()
      const elemento = linea.replace(lista === 'numerada' ? NUMERADO : VINETA, '').trim()
      const ultimo = resultado.at(-1)
      if (ultimo && ultimo.tipo === lista) ultimo.elementos.push(elemento)
      else resultado.push({ tipo: lista, elementos: [elemento] })
    } else if (!linea.trim()) {
      cerrarParrafo()
    } else {
      const ultimo = resultado.at(-1)
      // Una línea que sigue a un elemento de lista sin dejar renglón en blanco es parte de ese elemento
      if (parrafo.length === 0 && ultimo && ultimo.tipo !== 'parrafo' && /^\s+/.test(linea)) {
        ultimo.elementos[ultimo.elementos.length - 1] += ` ${linea.trim()}`
      } else {
        parrafo.push(linea.trim().replace(/^#+\s+/, ''))
      }
    }
  }
  cerrarParrafo()
  return resultado
}

/** Los trozos de una línea: lo que va entre `**` es negrita. */
export function trozos(linea: string): { texto: string; negrita: boolean }[] {
  return linea
    .split(/(\*\*[^*]+\*\*)/)
    .filter(Boolean)
    .map((trozo) =>
      trozo.startsWith('**') && trozo.endsWith('**') && trozo.length > 4
        ? { texto: trozo.slice(2, -2), negrita: true }
        : { texto: trozo, negrita: false })
}
