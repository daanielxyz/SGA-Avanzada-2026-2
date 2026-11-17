import { lazy, Suspense, useEffect, useState } from 'react'
import { Route, Routes } from 'react-router'
import { consultarAlojamiento, type Alojamiento } from './api/alojamiento'
import AlCambiarDePagina from './AlCambiarDePagina'
import Encabezado from './Encabezado'
import { aplicarMarca } from './marca'
import Apartamento from './paginas/Apartamento'
import DetalleReserva from './paginas/DetalleReserva'
import Entrar from './paginas/Entrar'
import Apartamentos from './paginas/Apartamentos'
import Inicio from './paginas/Inicio'
import MiCuenta from './paginas/MiCuenta'
import MisReservas from './paginas/MisReservas'
import NoEncontrada from './paginas/NoEncontrada'
import Politicas from './paginas/Politicas'
import PiePagina from './PiePagina'
import { ProveedorSesion, RutasDeHuesped } from './sesion/Sesion'

// Sus banderas y datos de teléfono pesan: solo se descargan al entrar a reservar
const Reservar = lazy(() => import('./paginas/Reservar'))

/**
 * Mapa de la web: la cabecera común y qué página se muestra para cada dirección. Al abrir pide los datos del
 * alojamiento (DEC-83): su nombre va a la cabecera y a la pestaña. Si la API no responde, la web sigue funcionando.
 */
export default function App() {
  const [alojamiento, setAlojamiento] = useState<Alojamiento | null>(null)

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    consultarAlojamiento()
      .then((a) => {
        if (vigente) {
          setAlojamiento(a)
          document.title = `${a.nombre} · Reservas`
          aplicarMarca(a.marca ?? null)
        }
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  const umbral = alojamiento?.parametros.umbralEdadFacturable ?? null

  return (
    <ProveedorSesion>
      <AlCambiarDePagina />
      <Encabezado nombre={alojamiento?.nombre ?? null} logoUrl={alojamiento?.marca?.logoUrl} />
      <Routes>
        <Route path="/" element={<Inicio alojamiento={alojamiento} />} />
        <Route path="/apartamentos" element={<Apartamentos alojamiento={alojamiento} />} />
        <Route path="/apartamentos/:codigo" element={<Apartamento umbral={umbral} />} />
        <Route path="/apartamentos/:codigo/reservar" element={(
          <Suspense fallback={<main className="ficha"><p className="suave">Cargando tu reserva…</p></main>}>
            <Reservar alojamiento={alojamiento} />
          </Suspense>
        )} />
        <Route path="/entrar" element={<Entrar />} />
        <Route path="/politicas" element={<Politicas alojamiento={alojamiento} />} />
        <Route element={<RutasDeHuesped />}>
          <Route path="/mi-cuenta" element={<MiCuenta />} />
          <Route path="/mis-reservas" element={<MisReservas />} />
          <Route path="/mis-reservas/:codigo" element={<DetalleReserva alojamiento={alojamiento} />} />
        </Route>
        <Route path="*" element={<NoEncontrada />} />
      </Routes>
      <PiePagina alojamiento={alojamiento} />
    </ProveedorSesion>
  )
}
