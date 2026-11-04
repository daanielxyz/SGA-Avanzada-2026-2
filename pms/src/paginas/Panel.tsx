import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { consultarAlojamiento } from '../api/alojamiento'
import Icono from '../componentes/Icono'
import Logo from '../componentes/Logo'
import { menuPara } from '../navegacion'
import type { Sesion } from '../sesion/almacen'
import { useSesion } from '../sesion/contexto'
import { NOMBRE_DEL_ROL } from '../sesion/roles'

// Desde este ancho el menú queda fijo a la izquierda (el mismo valor de src/index.css)
const ANCHO_MENU_FIJO = 800

/** Las dos primeras letras del nombre para el círculo de la cabecera. */
function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('')
}

/** Marco de todas las pantallas con sesión: el alojamiento, quién entró, el menú de su rol y el botón para salir. */
export default function Panel({ sesion }: { sesion: Sesion }) {
  const { salir } = useSesion()
  const [alojamiento, setAlojamiento] = useState('')
  const [menuAbierto, setMenuAbierto] = useState(false)
  const abrirMenu = useRef<HTMLButtonElement>(null)
  const cerrarMenu = useRef<HTMLButtonElement>(null)
  const rol = NOMBRE_DEL_ROL[sesion.rol]

  // Sin el nombre del alojamiento el PMS sigue funcionando: no se muestra y ya
  useEffect(() => {
    let vigente = true
    consultarAlojamiento()
      .then(({ nombre }) => {
        if (vigente) setAlojamiento(nombre)
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  // Abierto en el celular: el foco entra al menú; se cierra con Escape y el foco vuelve al botón que lo abrió
  useEffect(() => {
    if (!menuAbierto) return
    cerrarMenu.current?.focus()
    const botonParaVolver = abrirMenu.current
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') setMenuAbierto(false)
    }
    const alCambiarTamano = () => {
      if (window.innerWidth >= ANCHO_MENU_FIJO) setMenuAbierto(false)
    }
    document.addEventListener('keydown', alTeclear)
    window.addEventListener('resize', alCambiarTamano)
    return () => {
      document.removeEventListener('keydown', alTeclear)
      window.removeEventListener('resize', alCambiarTamano)
      botonParaVolver?.focus()
    }
  }, [menuAbierto])

  return (
    <div className="panel">
      <header className="cabecera" inert={menuAbierto}>
        <button
          ref={abrirMenu}
          type="button"
          className="boton boton-secundario boton-icono abrir-menu"
          aria-label="Abrir menú"
          aria-expanded={menuAbierto}
          aria-controls="menu-principal"
          onClick={() => setMenuAbierto(true)}
        >
          <Icono nombre="menu" />
        </button>
        <NavLink to="/" className="logo">
          <Logo />
        </NavLink>
        <span className="cabecera-separador" aria-hidden="true" />
        <p className="alojamiento" title={alojamiento}>
          {alojamiento}
        </p>
        <span className="avatar avatar-cabecera" aria-hidden="true">
          {iniciales(sesion.nombre)}
        </span>
        <p className="quien">
          <span className="quien-nombre">{sesion.nombre}</span>
          <span className="quien-rol">{rol}</span>
        </p>
        <button type="button" className="boton boton-secundario boton-chico cabecera-salir" onClick={() => void salir()}>
          Cerrar sesión
        </button>
      </header>

      {menuAbierto && <div className="velo" aria-hidden="true" onClick={() => setMenuAbierto(false)} />}

      <nav id="menu-principal" aria-label="Menú principal" className={menuAbierto ? 'menu abierto' : 'menu'}>
        {menuAbierto && (
          <>
            <div className="menu-cabecera">
              <button
                ref={cerrarMenu}
                type="button"
                className="boton boton-secundario boton-icono"
                aria-label="Cerrar menú"
                onClick={() => setMenuAbierto(false)}
              >
                <Icono nombre="cerrar" />
              </button>
              <Logo decorativo />
            </div>
            <div className="menu-persona">
              <span className="avatar" aria-hidden="true">
                {iniciales(sesion.nombre)}
              </span>
              <div>
                <p className="menu-persona-nombre">{sesion.nombre}</p>
                <p className="menu-persona-dato">{rol}</p>
                <p className="menu-persona-dato">{alojamiento}</p>
              </div>
            </div>
          </>
        )}

        {menuPara(sesion.rol).map(({ grupo, opciones }) => (
          <section key={grupo}>
            <h2>{grupo}</h2>
            <ul>
              {opciones.map((opcion) => (
                <li key={opcion.ruta}>
                  <NavLink to={opcion.ruta} onClick={() => setMenuAbierto(false)}>
                    <Icono nombre={opcion.icono} />
                    {opcion.titulo}
                  </NavLink>
                </li>
              ))}
            </ul>
          </section>
        ))}

        {menuAbierto && (
          <div className="menu-pie">
            <button
              type="button"
              className="boton boton-secundario boton-bloque boton-grande"
              onClick={() => void salir()}
            >
              Cerrar sesión
            </button>
          </div>
        )}
      </nav>

      <main className="contenido" inert={menuAbierto}>
        <div className="contenido-ancho">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
