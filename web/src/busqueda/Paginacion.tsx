import type { Pagina } from '../api/apartamentos'

interface Props {
  pagina: Pagina<unknown>
  alCambiar: (numero: number) => void
}

/** Anterior y siguiente bajo un listado de 10 en 10 (DEC-18); con una sola página no se muestra. */
export default function Paginacion({ pagina: { numero, totalPaginas }, alCambiar }: Props) {
  if (totalPaginas <= 1) {
    return null
  }
  return (
    <nav className="paginacion" aria-label="Páginas">
      <button type="button" className="secundario" disabled={numero === 0} onClick={() => alCambiar(numero - 1)}>
        ← Anterior
      </button>
      <span className="suave">
        Página {numero + 1} de {totalPaginas}
      </span>
      <button type="button" className="secundario" disabled={numero >= totalPaginas - 1}
        onClick={() => alCambiar(numero + 1)}>
        Siguiente →
      </button>
    </nav>
  )
}
