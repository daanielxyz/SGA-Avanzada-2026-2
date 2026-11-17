import { Link } from 'react-router'
import { escrito, type Alojamiento } from './api/alojamiento'

// El año de los derechos se toma al abrir la web, no en cada dibujo
const ANIO = new Date().getFullYear()

/**
 * Pie de todas las páginas, en columnas: el hotel (nombre, dirección y su Registro Nacional de Turismo, público ·
 * DEC-130), los atajos para reservar y las políticas. Lo que el hotel no ha escrito no se muestra (DEC-83).
 */
export default function PiePagina({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const direccion = escrito(alojamiento?.direccion)
  const ciudad = escrito(alojamiento?.ciudad)
  const lugar = [direccion, ciudad && !direccion?.includes(ciudad) ? ciudad : null].filter(Boolean).join(', ')
  const rnt = escrito(alojamiento?.rnt)
  return (
    <footer className="pie">
      <div className="pie-contenido">
        <div className="pie-hotel">
          {alojamiento && <p className="pie-nombre">{alojamiento.nombre}</p>}
          {lugar && <address>{lugar}</address>}
          {rnt && <p>Registro Nacional de Turismo {rnt}</p>}
        </div>
        <nav aria-labelledby="pie-reservas">
          <h2 id="pie-reservas">Reservas</h2>
          <Link to="/apartamentos">Apartamentos</Link>
          <Link to="/mis-reservas">Mis reservas</Link>
          <Link to="/mi-cuenta">Mi cuenta</Link>
        </nav>
        <nav aria-labelledby="pie-politicas">
          <h2 id="pie-politicas">Políticas</h2>
          <Link to="/politicas#cancelacion">Cancelación</Link>
          <Link to="/politicas#datos">Tratamiento de datos</Link>
          <Link to="/politicas#cookies">Cookies</Link>
        </nav>
      </div>
      {alojamiento && <p className="pie-derechos">© {ANIO} {alojamiento.nombre}</p>}
    </footer>
  )
}
