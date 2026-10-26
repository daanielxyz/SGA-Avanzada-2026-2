import { Route, Routes } from 'react-router'
import Inicio from './paginas/Inicio'
import NoEncontrada from './paginas/NoEncontrada'

/** Mapa de la web: qué página se muestra para cada dirección. */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  )
}
