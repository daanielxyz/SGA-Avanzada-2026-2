import { createContext, useContext } from 'react'
import type { Sesion } from './almacen'

/**
 * Por qué no hay sesión: nunca se entró, se salió a propósito, el backend dejó de aceptar el token o la persona cambió
 * algo de su cuenta que obliga a volver a entrar (su contraseña o su rol).
 */
export type MotivoSinSesion = 'sin-entrar' | 'salio' | 'perdida' | 'cuenta-cambiada'

export interface ValorSesion {
  sesion: Sesion | null
  motivo: MotivoSinSesion
  /** Lo que se le dice al volver a «Iniciar sesión» cuando el motivo es `cuenta-cambiada`. */
  aviso: string | null
  entrar: (correo: string, contrasena: string) => Promise<void>
  salir: () => Promise<void>
  /** Cierra la sesión en esta pestaña porque el backend ya no la acepta, y deja el aviso para la entrada. */
  terminar: (aviso: string) => void
  /** La persona cambió su nombre: la cabecera lo muestra sin volver a entrar. */
  renombrar: (nombre: string) => void
}

export const ContextoSesion = createContext<ValorSesion | null>(null)

/** La sesión del personal; solo funciona dentro de `ProveedorSesion`. */
export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion)
  if (!valor) throw new Error('useSesion se usa dentro de ProveedorSesion')
  return valor
}
