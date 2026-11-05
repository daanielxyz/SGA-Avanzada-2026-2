// Dónde queda la sesión del huésped. Se guarda en sessionStorage y no en localStorage: un huésped puede entrar desde
// un computador compartido, y así termina al cerrar la pestaña. El backend igual revisa el token (DEC-71).

/** Lo que la web necesita de `SesionResponse` (`POST /api/auth/huesped/login`). */
export interface Sesion {
  token: string
  expiraEn: string
  nombre: string
}

const CLAVE = 'sga.web.sesion'

let enMemoria: Sesion | null = null
let alPerder: (() => void) | null = null

function esSesion(valor: unknown): valor is Sesion {
  if (typeof valor !== 'object' || valor === null) return false
  const { token, expiraEn, nombre } = valor as Partial<Sesion>
  return typeof token === 'string' && typeof expiraEn === 'string' && typeof nombre === 'string'
}

/** La sesión guardada, o `null` si no hay, ya venció o no se puede leer. */
export function leerSesion(ahora: Date = new Date()): Sesion | null {
  let guardada: unknown = enMemoria
  try {
    guardada = enMemoria ?? JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null')
  } catch {
    // Sin almacenamiento o con algo ilegible, queda lo que haya en memoria
  }
  if (!esSesion(guardada) || new Date(guardada.expiraEn) <= ahora) {
    borrarSesion()
    return null
  }
  return guardada
}

export function guardarSesion(sesion: Sesion) {
  enMemoria = sesion
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    // Sin almacenamiento (navegación privada estricta), la sesión dura lo que dure la página abierta
  }
}

export function borrarSesion() {
  enMemoria = null
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar
  }
}

/** Registra qué hacer cuando el backend deja de aceptar el token; devuelve cómo dejar de escuchar. */
export function alPerderSesion(accion: () => void): () => void {
  alPerder = accion
  return () => {
    alPerder = null
  }
}

/** El backend rechazó el token: se borra y se avisa a la pantalla. */
export function perderSesion() {
  borrarSesion()
  alPerder?.()
}
