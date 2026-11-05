import { createContext, use } from 'react'
import type { Sesion } from './almacen'

export interface ValorSesion {
  sesion: Sesion | null
  /** El backend dejó de aceptar la sesión: la página de entrar lo explica. */
  perdida: boolean
  entrar: (sesion: Sesion) => void
  salir: () => void
}

export const ContextoSesion = createContext<ValorSesion | null>(null)

/** Quién entró, para la cabecera y las páginas del huésped. */
export function useSesion() {
  const valor = use(ContextoSesion)
  if (!valor) throw new Error('useSesion necesita ProveedorSesion')
  return valor
}
