// Dónde queda la sesión del personal mientras el token vale. Se guarda en localStorage para que recepción pueda
// abrir varias pestañas sin volver a entrar; el backend igual revisa el token en cada petición (DEC-71).
import { esRolDelPersonal, type Rol } from './roles'

/** Lo que el PMS necesita de `SesionResponse` (`POST /api/auth/login`). */
export interface Sesion {
  token: string
  expiraEn: string
  usuario: string
  nombre: string
  rol: Rol
}

const CLAVE = 'sga.pms.sesion'

let alPerder: (() => void) | null = null

/** La sesión guardada, o `null` si no hay, ya venció o no se puede leer. */
export function leerSesion(ahora: Date = new Date()): Sesion | null {
  try {
    const guardada: unknown = JSON.parse(localStorage.getItem(CLAVE) ?? 'null')
    if (!esSesion(guardada) || new Date(guardada.expiraEn) <= ahora) {
      localStorage.removeItem(CLAVE)
      return null
    }
    return guardada
  } catch {
    return null
  }
}

export function guardarSesion(sesion: Sesion) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    // Sin almacenamiento (navegación privada estricta) la sesión dura lo que dure la pestaña en memoria
  }
}

export function borrarSesion() {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar
  }
}

/** Registra qué hacer cuando el backend deja de aceptar el token; devuelve cómo dejar de escuchar. */
export function alPerderSesion(accion: () => void): () => void {
  alPerder = accion
  return () => {
    if (alPerder === accion) alPerder = null
  }
}

/** El backend respondió 401 a un token: venció, se cerró o al usuario lo eliminaron o le cambiaron el rol. */
export function sesionPerdida() {
  borrarSesion()
  alPerder?.()
}

function esSesion(valor: unknown): valor is Sesion {
  if (typeof valor !== 'object' || valor === null) return false
  const s = valor as Record<string, unknown>
  return typeof s.token === 'string' && typeof s.expiraEn === 'string' && typeof s.usuario === 'string'
    && typeof s.nombre === 'string' && typeof s.rol === 'string' && esRolDelPersonal(s.rol)
}
