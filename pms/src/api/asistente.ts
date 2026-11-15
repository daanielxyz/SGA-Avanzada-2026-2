// El asistente de dudas del PMS (DEC-127): `POST /api/asistente/preguntas`. Siempre responde 200; si no respondió el
// asistente sino el manual, lo dice en `origen`, `motivo` y `aviso`.
import { pedir } from './cliente'

export type AutorMensaje = 'PERSONA' | 'ASISTENTE'

export interface MensajeAnterior {
  autor: AutorMensaje
  texto: string
}

export interface FuenteRespuesta {
  id: string
  titulo: string
  /** La ruta del PMS; puede traer `{codigo}` donde va el código de la reserva o del apartamento. */
  pantalla: string
}

export interface RespuestaAsistente {
  /** Markdown sencillo: párrafos, listas y negritas. */
  respuesta: string
  fuentes: FuenteRespuesta[]
  origen: 'IA' | 'LOCAL'
  motivo: string | null
  /** Solo si respondió el manual: por qué, dicho para la persona. Se muestra tal cual. */
  aviso: string | null
}

export function preguntar(pregunta: string, pantalla: string, historial: MensajeAnterior[]): Promise<RespuestaAsistente> {
  return pedir<RespuestaAsistente>('/api/asistente/preguntas', {
    method: 'POST',
    cuerpo: { pregunta, pantalla, historial },
  })
}
