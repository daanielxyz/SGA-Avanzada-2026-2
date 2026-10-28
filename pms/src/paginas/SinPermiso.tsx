import { Link } from 'react-router'

/** Se muestra cuando alguien abre la dirección de una pantalla que su rol no puede usar (403 `SIN_PERMISO`). */
export default function SinPermiso() {
  return (
    <>
      <h1>No tienes permiso para esta pantalla</h1>
      <p className="suave">Si la necesitas, pídele al administrador que revise tu rol.</p>
      <Link to="/">Volver al inicio</Link>
    </>
  )
}
