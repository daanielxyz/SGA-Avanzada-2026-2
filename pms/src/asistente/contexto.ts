import { createContext, useContext } from 'react'
import type { Intercambio } from './logica'

export interface ValorAsistente {
  abierto: boolean
  intercambios: readonly Intercambio[]
  abrir: () => void
  cerrar: () => void
  /** Abre el panel y envía la pregunta, como si se escribiera. */
  preguntar: (pregunta: string) => void
  reintentar: (id: number) => void
  empezarDeNuevo: () => void
}

export const ContextoAsistente = createContext<ValorAsistente | null>(null)

/** El asistente del PMS; nulo fuera de las pantallas con sesión (por ejemplo, en «Iniciar sesión»). */
export function useAsistente(): ValorAsistente | null {
  return useContext(ContextoAsistente)
}
