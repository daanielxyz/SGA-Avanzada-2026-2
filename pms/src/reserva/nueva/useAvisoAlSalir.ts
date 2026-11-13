import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

/**
 * Mientras haya algo a medio llenar, pregunta antes de seguir un enlace, con un diálogo de la página, y antes de
 * recargar o cerrar la pestaña, con el aviso del navegador (ese no se puede cambiar). Los datos de las personas no se
 * guardan en el navegador, así que al salir se pierden.
 *
 * @returns el destino que espera respuesta (nulo si no se está preguntando), y cómo salir o quedarse
 */
export function useAvisoAlSalir(activo: boolean) {
  const navegar = useNavigate()
  const [destino, setDestino] = useState<string | null>(null)

  useEffect(() => {
    if (!activo) return
    function alCerrar(evento: BeforeUnloadEvent) {
      evento.preventDefault()
    }
    function alHacerClic(evento: MouseEvent) {
      const enlace = (evento.target as Element | null)?.closest?.('a[href]')
      if (!(enlace instanceof HTMLAnchorElement)) return
      // Un enlace que abre otra pestaña o va a otro sitio no saca a nadie de aquí
      if (enlace.target === '_blank' || enlace.origin !== window.location.origin) return
      evento.preventDefault()
      evento.stopPropagation()
      setDestino(enlace.pathname + enlace.search + enlace.hash)
    }
    window.addEventListener('beforeunload', alCerrar)
    document.addEventListener('click', alHacerClic, true)
    return () => {
      window.removeEventListener('beforeunload', alCerrar)
      document.removeEventListener('click', alHacerClic, true)
    }
  }, [activo])

  return {
    destino: activo ? destino : null,
    salir() {
      if (destino) navegar(destino)
      setDestino(null)
    },
    quedarse() {
      setDestino(null)
    },
  }
}
