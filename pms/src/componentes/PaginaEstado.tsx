import { Link } from 'react-router'
import Icono, { type NombreIcono } from './Icono'
import { useTitulo } from './useTitulo'

/** Plantilla única de las páginas que solo explican algo: sin permiso, en construcción y no encontrada. */
export default function PaginaEstado({ icono, titulo, texto, accion = 'Volver al inicio', recargar = false }: {
  icono: NombreIcono
  titulo: string
  texto: string
  accion?: string
  recargar?: boolean
}) {
  useTitulo(titulo)
  return (
    <section className="tarjeta pagina-estado">
      <div className="icono-estado">
        <Icono nombre={icono} />
      </div>
      <h1>{titulo}</h1>
      <p className="suave">{texto}</p>
      <Link to="/" className="boton" reloadDocument={recargar}>
        {accion}
      </Link>
    </section>
  )
}
