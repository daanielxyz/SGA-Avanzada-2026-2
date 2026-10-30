import { useEffect, useState } from 'react'
import { Route, Routes } from 'react-router'
import { consultarAlojamiento, type Alojamiento } from './api/alojamiento'
import Encabezado from './Encabezado'
import Apartamento from './paginas/Apartamento'
import Inicio from './paginas/Inicio'
import NoEncontrada from './paginas/NoEncontrada'

/**
 * Mapa de la web: la cabecera común y qué página se muestra para cada dirección. Al abrir pide los datos del
 * alojamiento (DEC-83): su nombre va a la cabecera y a la pestaña. Si la API no responde, la web sigue funcionando.
 */
export default function App() {
  const [alojamiento, setAlojamiento] = useState<Alojamiento | null>(null)

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    consultarAlojamiento()
      .then((a) => {
        if (vigente) {
          setAlojamiento(a)
          document.title = `${a.nombre} · Reservas`
        }
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  const umbral = alojamiento?.parametros.umbralEdadFacturable ?? null

  return (
    <>
      <Encabezado nombre={alojamiento?.nombre ?? null} />
      <Routes>
        <Route path="/" element={<Inicio umbral={umbral} />} />
        <Route path="/apartamentos/:codigo" element={<Apartamento umbral={umbral} />} />
        <Route path="*" element={<NoEncontrada />} />
      </Routes>
    </>
  )
}
