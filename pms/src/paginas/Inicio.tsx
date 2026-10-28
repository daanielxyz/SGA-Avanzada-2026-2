import { Link } from 'react-router'
import { menuPara } from '../navegacion'
import { useSesion } from '../sesion/contexto'

/** Primera pantalla después de entrar: saludo y accesos a lo que el rol puede hacer. */
export default function Inicio() {
  const { sesion } = useSesion()
  if (!sesion) return null

  return (
    <>
      <h1>Hola, {sesion.nombre}</h1>
      <p className="suave">Elige qué quieres hacer.</p>
      <div className="accesos">
        {menuPara(sesion.rol).flatMap(({ opciones }) =>
          opciones.map((opcion) => (
            <Link key={opcion.ruta} to={opcion.ruta} className="acceso">
              {opcion.titulo}
            </Link>
          )),
        )}
      </div>
    </>
  )
}
