import { useEffect } from 'react'

const PREGUNTA = 'Tienes una reserva a medio llenar. Si sales, se pierde lo escrito. ¿Quieres salir?'

/**
 * Mientras haya algo a medio llenar, pregunta antes de recargar, cerrar la pestaña o seguir un enlace. Los datos de
 * las personas no se guardan en el navegador, así que al salir se pierden.
 */
export function useAvisoAlSalir(activo: boolean) {
  useEffect(() => {
    if (!activo) return
    function alCerrar(evento: BeforeUnloadEvent) {
      evento.preventDefault()
    }
    function alHacerClic(evento: MouseEvent) {
      const enlace = (evento.target as Element | null)?.closest?.('a[href]')
      if (!enlace || window.confirm(PREGUNTA)) return
      evento.preventDefault()
      evento.stopPropagation()
    }
    window.addEventListener('beforeunload', alCerrar)
    document.addEventListener('click', alHacerClic, true)
    return () => {
      window.removeEventListener('beforeunload', alCerrar)
      document.removeEventListener('click', alHacerClic, true)
    }
  }, [activo])
}
