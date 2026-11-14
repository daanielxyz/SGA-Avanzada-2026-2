import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import ProteccionErrores from './componentes/ProteccionErrores'
import { ADMINISTRACION, OPCIONES, RECEPCION } from './navegacion'
import ActivarCuenta from './paginas/ActivarCuenta'
import Alojamiento from './paginas/Alojamiento'
import Apartamentos from './paginas/Apartamentos'
import DetalleReserva from './paginas/DetalleReserva'
import EnConstruccion from './paginas/EnConstruccion'
import FichaApartamento from './paginas/FichaApartamento'
import Folio from './paginas/Folio'
import IniciarSesion from './paginas/IniciarSesion'
import Inicio from './paginas/Inicio'
import Llegada from './paginas/Llegada'
import ListadoReservas from './paginas/ListadoReservas'
import NoEncontrada from './paginas/NoEncontrada'
import NuevaReserva from './paginas/NuevaReserva'
import Ocupacion from './paginas/Ocupacion'
import Panel from './paginas/Panel'
import Perfil from './paginas/Perfil'
import Politica from './paginas/Politica'
import Preparacion from './paginas/Preparacion'
import PublicarPolitica from './paginas/PublicarPolitica'
import RecuperarContrasena from './paginas/RecuperarContrasena'
import SinPermiso from './paginas/SinPermiso'
import Tarifas from './paginas/Tarifas'
import Usuarios from './paginas/Usuarios'
import { useSesion } from './sesion/contexto'
import type { Rol } from './sesion/roles'

// Las pantallas del menú que ya existen; las demás llegan en los sprints siguientes (docs/SPRINTS.md)
const PANTALLAS: Partial<Record<string, ReactNode>> = {
  '/reservas': <ListadoReservas />,
  '/ocupacion': <Ocupacion />,
  '/preparacion': <Preparacion />,
  '/alojamiento': <Alojamiento />,
  '/politica': <Politica />,
  '/apartamentos': <Apartamentos />,
  '/tarifas': <Tarifas />,
  '/usuarios': <Usuarios />,
  '/perfil': <Perfil />,
}

/** Mapa del PMS: qué pantalla se muestra para cada dirección y quién la puede abrir. */
export default function App() {
  return (
    <ProteccionErrores global>
    <Routes>
      <Route path="/ingresar" element={<IniciarSesion />} />
      <Route path="/activar" element={<ActivarCuenta />} />
      <Route path="/recuperar" element={<RecuperarContrasena />} />
      <Route element={<ConSesion />}>
        <Route index element={<Inicio />} />
        {OPCIONES.map((opcion) => (
          <Route
            key={opcion.ruta}
            path={opcion.ruta}
            element={
              <SoloPara roles={opcion.roles}>
                {PANTALLAS[opcion.ruta] ?? <EnConstruccion titulo={opcion.titulo} />}
              </SoloPara>
            }
          />
        ))}
        <Route
          path="/reservas/nueva"
          element={
            <SoloPara roles={RECEPCION}>
              <NuevaReserva />
            </SoloPara>
          }
        />
        <Route
          path="/reservas/:codigo"
          element={
            <SoloPara roles={RECEPCION}>
              <DetalleReserva />
            </SoloPara>
          }
        />
        <Route
          path="/reservas/:codigo/folio"
          element={
            <SoloPara roles={RECEPCION}>
              <Folio />
            </SoloPara>
          }
        />
        <Route
          path="/reservas/:codigo/llegada"
          element={
            <SoloPara roles={RECEPCION}>
              <Llegada />
            </SoloPara>
          }
        />
        <Route
          path="/politica/publicar"
          element={
            <SoloPara roles={['ADMINISTRADOR']}>
              <PublicarPolitica />
            </SoloPara>
          }
        />
        <Route
          path="/apartamentos/:codigo"
          element={
            <SoloPara roles={ADMINISTRACION}>
              <FichaApartamento />
            </SoloPara>
          }
        />
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
    </ProteccionErrores>
  )
}

/**
 * Sin sesión, toda pantalla manda a entrar. Si la sesión se perdió, recuerda adónde se quería ir; si la persona salió a
 * propósito, no: quien entre después, que puede ser otra persona, empieza en Inicio.
 */
function ConSesion() {
  const { sesion, motivo } = useSesion()
  const { pathname, search } = useLocation()
  if (!sesion) {
    // Tras cambiar la contraseña o el propio rol se vuelve a entrar desde Inicio
    const recordar = motivo === 'sin-entrar' || motivo === 'perdida'
    return <Navigate to="/ingresar" replace state={recordar ? { desde: pathname + search } : null} />
  }
  return <Panel sesion={sesion} />
}

/** Lo mismo que hace el backend con 403, para no mostrar una pantalla que igual no va a funcionar. */
function SoloPara({ roles, children }: { roles: readonly Rol[]; children: ReactNode }) {
  const { sesion } = useSesion()
  return sesion && roles.includes(sesion.rol) ? children : <SinPermiso />
}
