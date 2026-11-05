import { startTransition } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { cerrarSesion } from './api/cuenta'
import { useSesion } from './sesion/useSesion'

interface Props {
  nombre: string | null
  logoUrl?: string | null
}

/**
 * Cabecera de todas las páginas: el logo y el nombre del alojamiento, vacíos mientras llegan o si la API no responde,
 * y el acceso a la cuenta del huésped.
 */
export default function Encabezado({ nombre, logoUrl }: Props) {
  const { sesion, salir } = useSesion()
  const navegar = useNavigate()

  async function alSalir() {
    // Pase lo que pase en el backend, en este navegador la sesión termina
    await cerrarSesion().catch(() => {})
    // Juntos: sin sesión, una página del huésped alcanzaría a llevar a entrar antes de cambiar de página
    startTransition(() => {
      navegar('/')
      salir()
    })
  }

  return (
    <header className="encabezado">
      {nombre && (
        <Link to="/" className="marca" title={nombre}>
          {logoUrl && <img src={logoUrl} alt="" />}
          <span>{nombre}</span>
        </Link>
      )}
      <nav className="menu-cuenta" aria-label="Tu cuenta">
        {sesion ? (
          <>
            <NavLink to="/mis-reservas">Mis reservas</NavLink>
            <NavLink to="/mi-cuenta">Mi cuenta</NavLink>
            <button type="button" className="secundario" onClick={alSalir}>Salir</button>
          </>
        ) : (
          <NavLink to="/entrar">Entrar</NavLink>
        )}
      </nav>
    </header>
  )
}
