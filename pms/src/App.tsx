import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { OPCIONES } from './navegacion'
import EnConstruccion from './paginas/EnConstruccion'
import IniciarSesion from './paginas/IniciarSesion'
import Inicio from './paginas/Inicio'
import NoEncontrada from './paginas/NoEncontrada'
import Panel from './paginas/Panel'
import SinPermiso from './paginas/SinPermiso'
import { useSesion } from './sesion/contexto'
import type { Rol } from './sesion/roles'

/** Mapa del PMS: qué pantalla se muestra para cada dirección y quién la puede abrir. */
export default function App() {
  return (
    <Routes>
      <Route path="/ingresar" element={<IniciarSesion />} />
      <Route element={<ConSesion />}>
        <Route index element={<Inicio />} />
        {OPCIONES.map((opcion) => (
          <Route
            key={opcion.ruta}
            path={opcion.ruta}
            element={
              <SoloPara roles={opcion.roles}>
                <EnConstruccion titulo={opcion.titulo} />
              </SoloPara>
            }
          />
        ))}
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  )
}

/** Sin sesión, toda pantalla manda a entrar y recuerda adónde se quería ir. */
function ConSesion() {
  const { sesion } = useSesion()
  const { pathname } = useLocation()
  if (!sesion) {
    return <Navigate to="/ingresar" replace state={{ desde: pathname }} />
  }
  return <Panel sesion={sesion} />
}

/** Lo mismo que hace el backend con 403, para no mostrar una pantalla que igual no va a funcionar. */
function SoloPara({ roles, children }: { roles: readonly Rol[]; children: ReactNode }) {
  const { sesion } = useSesion()
  return sesion && roles.includes(sesion.rol) ? children : <SinPermiso />
}
