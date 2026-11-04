import { Link } from 'react-router'

interface Props {
  nombre: string | null
  logoUrl?: string | null
}

/** Cabecera de todas las páginas con el logo y el nombre del alojamiento; vacía mientras llega o si la API no responde. */
export default function Encabezado({ nombre, logoUrl }: Props) {
  return (
    <header className="encabezado">
      {nombre && (
        <Link to="/" className="marca" title={nombre}>
          {logoUrl && <img src={logoUrl} alt="" />}
          <span>{nombre}</span>
        </Link>
      )}
    </header>
  )
}
