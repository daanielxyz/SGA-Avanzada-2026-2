import { Link } from 'react-router'

/** Se muestra cuando la dirección no corresponde a ninguna pantalla. */
export default function NoEncontrada() {
  return (
    <>
      <h1>No encontramos esta página</h1>
      <Link to="/">Volver al inicio</Link>
    </>
  )
}
