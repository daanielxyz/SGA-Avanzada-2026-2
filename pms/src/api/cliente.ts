// Única puerta del PMS hacia la API: toda llamada pasa por aquí para que el token y los errores se traten igual en
// todas las pantallas. Es el mismo cliente de web/, más el token del personal.
import { leerSesion, sesionPerdida } from '../sesion/almacen'

/** Cuerpo uniforme de los errores del backend (`ErrorResponse`, DEC-33). */
export interface ErrorRespuesta {
  estado: number
  codigo: string
  mensaje: string
  detalles: string[]
}

/** Lo que falla al hablar con la API: un error del backend o que el backend no respondió. */
export class ErrorApi extends Error {
  readonly estado: number
  readonly codigo: string
  readonly detalles: string[]

  constructor({ estado, codigo, mensaje, detalles }: ErrorRespuesta) {
    super(mensaje)
    this.name = 'ErrorApi'
    this.estado = estado
    this.codigo = codigo
    this.detalles = detalles
  }
}

const SIN_CONEXION: ErrorRespuesta = {
  estado: 0,
  codigo: 'SIN_CONEXION',
  mensaje: 'No pudimos comunicarnos con el servidor. Intenta de nuevo en un momento.',
  detalles: [],
}

// Vacío en desarrollo: las rutas /api las reenvía Vite (vite.config.ts). En un servidor real se define al construir.
const BASE = import.meta.env.VITE_API_URL ?? ''

/**
 * Llama a la API con el token de la sesión, si hay, y devuelve el JSON de la respuesta.
 *
 * @param ruta     empieza por /api, por ejemplo `/api/reservas?pagina=0`
 * @param opciones lo mismo que acepta `fetch`; si trae `cuerpo`, se envía como JSON
 * @throws ErrorApi con el error del backend, o con `SIN_CONEXION` si no respondió o no respondió JSON. Un 401 a una
 *         petición con token cierra además la sesión del PMS (DEC-71).
 */
export async function pedir<T>(ruta: string, opciones: RequestInit & { cuerpo?: unknown } = {}): Promise<T> {
  const { cuerpo, headers, ...resto } = opciones
  const token = leerSesion()?.token
  let respuesta: Response
  try {
    respuesta = await fetch(BASE + ruta, {
      ...resto,
      headers: { Accept: 'application/json', ...(cuerpo === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
      body: cuerpo === undefined ? resto.body : JSON.stringify(cuerpo),
    })
  } catch {
    throw new ErrorApi(SIN_CONEXION)
  }

  if (respuesta.status === 401 && token) {
    sesionPerdida()
  }
  if (respuesta.status === 204) {
    return undefined as T
  }
  const json: unknown = await respuesta.json().catch(() => undefined)
  if (respuesta.ok && json !== undefined) {
    return json as T
  }
  throw new ErrorApi(esErrorRespuesta(json) ? json : SIN_CONEXION)
}

function esErrorRespuesta(valor: unknown): valor is ErrorRespuesta {
  return typeof valor === 'object' && valor !== null && 'codigo' in valor && 'mensaje' in valor
}
