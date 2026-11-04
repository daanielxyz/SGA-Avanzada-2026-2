import { useEffect } from 'react'

/** El título de la pestaña dice en qué pantalla se está; es lo primero que anuncia un lector de pantalla. */
export function useTitulo(titulo: string) {
  useEffect(() => {
    document.title = `${titulo} · Meridian`
  }, [titulo])
}
