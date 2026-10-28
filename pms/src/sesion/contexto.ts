import { createContext, useContext } from 'react'
import type { Sesion } from './almacen'

/** Por qué no hay sesión: nunca se entró, se salió a propósito o el backend dejó de aceptar el token. */
export type MotivoSinSesion = 'sin-entrar' | 'salio' | 'perdida'

export interface ValorSesion {
  sesion: Sesion | null
  motivo: MotivoSinSesion
  entrar: (correo: string, contrasena: string) => Promise<void>
  salir: () => Promise<void>
}

export const ContextoSesion = createContext<ValorSesion | null>(null)

/** La sesión del personal; solo funciona dentro de `ProveedorSesion`. */
export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion)
  if (!valor) throw new Error('useSesion se usa dentro de ProveedorSesion')
  return valor
}
