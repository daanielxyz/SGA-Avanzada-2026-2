import { NavLink, Outlet } from 'react-router'
import { menuPara } from '../navegacion'
import type { Sesion } from '../sesion/almacen'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_DEL_ROL } from '../sesion/roles'

/** Marco de todas las pantallas con sesión: quién entró, el menú de su rol y el botón para salir. */
export default function Panel({ sesion }: { sesion: Sesion }) {
  const { salir } = useSesion()

  return (
    <div className="panel">
      <header className="cabecera">
        <NavLink to="/" className="marca">
          SGA
        </NavLink>
        <p className="quien">
          {sesion.nombre} <span className="suave">· {NOMBRE_DEL_ROL[sesion.rol]}</span>
        </p>
        <button type="button" className="secundario" onClick={() => void salir()}>
          Salir
        </button>
      </header>

      <nav aria-label="Menú principal" className="menu">
        {menuPara(sesion.rol).map(({ grupo, opciones }) => (
          <section key={grupo}>
            <h2>{grupo}</h2>
            <ul>
              {opciones.map((opcion) => (
                <li key={opcion.ruta}>
                  <NavLink to={opcion.ruta}>{opcion.titulo}</NavLink>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </nav>

      <main className="contenido">
        <Outlet />
      </main>
    </div>
  )
}
