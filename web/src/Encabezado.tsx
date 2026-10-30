import { Link } from 'react-router'

/** Cabecera de todas las páginas con el nombre del alojamiento; vacía mientras llega o si la API no responde. */
export default function Encabezado({ nombre }: { nombre: string | null }) {
  return (
    <header className="encabezado">
      {nombre && (
        <Link to="/" className="marca">
          {nombre}
        </Link>
      )}
    </header>
  )
}
