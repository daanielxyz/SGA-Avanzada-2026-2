import PaginaEstado from '../componentes/PaginaEstado'

/** Lugar de una pantalla del menú que llega en un sprint siguiente (docs/SPRINTS.md). */
export default function EnConstruccion({ titulo }: { titulo: string }) {
  return <PaginaEstado icono="construccion" titulo={titulo} texto="Esta pantalla todavía está en construcción." />
}
