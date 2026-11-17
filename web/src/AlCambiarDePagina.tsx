import { useEffect } from 'react'
import { useLocation } from 'react-router'

/**
 * Al pasar a otra página, se ve desde arriba: el navegador no lo hace solo en una web de una sola página, y quien
 * toca «Ver apartamentos» al final de la presentación llegaría al final del listado. Con un ancla (`#cookies`), va a
 * esa parte.
 */
export default function AlCambiarDePagina() {
  const { pathname, hash } = useLocation()
  useEffect(() => {
    const destino = hash ? document.getElementById(decodeURIComponent(hash.slice(1))) : null
    if (destino) {
      destino.scrollIntoView({ block: 'start' })
    } else {
      window.scrollTo({ top: 0 })
    }
  }, [pathname, hash])
  return null
}
