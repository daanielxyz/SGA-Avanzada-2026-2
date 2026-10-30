import { Route, Routes } from 'react-router'
import Encabezado from './Encabezado'
import Inicio from './paginas/Inicio'
import NoEncontrada from './paginas/NoEncontrada'

/** Mapa de la web: la cabecera común y qué página se muestra para cada dirección. */
export default function App() {
  return (
    <>
      <Encabezado />
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="*" element={<NoEncontrada />} />
      </Routes>
    </>
  )
}
