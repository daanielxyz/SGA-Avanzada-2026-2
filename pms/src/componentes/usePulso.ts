import { useEffect, useState } from 'react'

/** Cada cuánto se vuelve a leer lo que otra persona pudo cambiar. */
export const CADA_CUANTO_REFRESCAR = 20_000

/**
 * Un número que sube cada cierto tiempo mientras la pestaña está a la vista, y también al volver a ella. Sirve de
 * dependencia de un efecto para volver a leer sin recargar: lo que hace el personal de servicio o el administrador en
 * otra pantalla llega solo (ronda 2). No es tiempo real: el backend no avisa, se le pregunta.
 */
export function usePulso(cadaMs = CADA_CUANTO_REFRESCAR): number {
  const [pulso, setPulso] = useState(0)
  useEffect(() => {
    function avanzar() {
      if (document.visibilityState === 'visible') setPulso((actual) => actual + 1)
    }
    const temporizador = window.setInterval(avanzar, cadaMs)
    document.addEventListener('visibilitychange', avanzar)
    return () => {
      window.clearInterval(temporizador)
      document.removeEventListener('visibilitychange', avanzar)
    }
  }, [cadaMs])
  return pulso
}
