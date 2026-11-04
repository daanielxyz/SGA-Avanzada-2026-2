import { Link } from 'react-router'
import Icono from '../componentes/Icono'
import { useTitulo } from '../componentes/useTitulo'
import { menuPara } from '../navegacion'
import { useSesion } from '../sesion/contexto'

/** Primera pantalla después de entrar: saludo y accesos a lo que el rol puede hacer. */
export default function Inicio() {
  const { sesion } = useSesion()
  useTitulo('Inicio')
  if (!sesion) return null

  return (
    <div className="bienvenida">
      <div className="bienvenida-saludo">
        <div className="icono-estado">
          <Icono nombre="alojamiento" />
        </div>
        <h1>Hola, {sesion.nombre}</h1>
        <p className="suave">Estos son los accesos de tu rol.</p>
      </div>
      <div className="accesos">
        {menuPara(sesion.rol).flatMap(({ opciones }) =>
          opciones.map((opcion) => (
            <Link key={opcion.ruta} to={opcion.ruta} className="acceso">
              <Icono nombre={opcion.icono} />
              <span>
                <span className="acceso-titulo">{opcion.titulo}</span>
                <span className="acceso-texto">{opcion.descripcion}</span>
              </span>
            </Link>
          )),
        )}
      </div>
    </div>
  )
}
