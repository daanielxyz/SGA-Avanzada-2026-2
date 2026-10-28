// Sesiones y respuestas de la API simuladas que comparten las pruebas.
import { vi } from 'vitest'
import { guardarSesion, type Sesion } from '../sesion/almacen'
import type { Rol } from '../sesion/roles'

export function sesionDe(rol: Rol, cambios: Partial<Sesion> = {}): Sesion {
  return {
    token: `token-${rol}`,
    expiraEn: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    usuario: 'USR-1',
    nombre: 'Ana Gómez',
    rol,
    ...cambios,
  }
}

/** Deja guardada una sesión como si la persona ya hubiera entrado. */
export function entrarComo(rol: Rol): Sesion {
  const sesion = sesionDe(rol)
  guardarSesion(sesion)
  return sesion
}

/** La API simulada responde, en orden, lo que se le pase: un estado HTTP y un cuerpo JSON. */
export function laApiResponde(...respuestas: [number, unknown][]) {
  const fetch = vi.fn()
  for (const [estado, cuerpo] of respuestas) {
    fetch.mockResolvedValueOnce(new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), { status: estado }))
  }
  vi.stubGlobal('fetch', fetch)
  return fetch
}

/** Dirección, método, cabeceras y cuerpo de la llamada número `n` a la API simulada. */
export function llamada(fetch: ReturnType<typeof vi.fn>, n: number) {
  const [url, opciones] = fetch.mock.calls[n] as [string, RequestInit | undefined]
  return {
    url,
    metodo: opciones?.method ?? 'GET',
    cabeceras: (opciones?.headers ?? {}) as Record<string, string>,
    cuerpo: opciones?.body ? JSON.parse(String(opciones.body)) : undefined,
  }
}
