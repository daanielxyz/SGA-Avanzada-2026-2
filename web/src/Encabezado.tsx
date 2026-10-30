import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { consultarAlojamiento } from './api/alojamiento'

/**
 * Cabecera de todas las páginas con el nombre del alojamiento, que también va a la pestaña del navegador (DEC-83).
 * Si la API no responde, la web sigue funcionando: la cabecera queda vacía y la pestaña dice «Reservas».
 */
export default function Encabezado() {
  const [nombre, setNombre] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    consultarAlojamiento()
      .then((alojamiento) => {
        if (vigente) {
          setNombre(alojamiento.nombre)
          document.title = `${alojamiento.nombre} · Reservas`
        }
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  return (
    <header className="encabezado">
      {nombre && (
        <Link to="/" className="marca">
          {nombre}
        </Link>
      )}
    </header>
  )
}
