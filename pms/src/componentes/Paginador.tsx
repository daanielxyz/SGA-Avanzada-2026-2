import { cantidad } from '../formato'

/**
 * «Página 2 de 7 · 64 reservas» con anterior y siguiente.
 *
 * @param numero empieza en 0, como en la API (DEC-18)
 */
export default function Paginador({ numero, totalPaginas, totalElementos, alCambiar }: {
  numero: number
  totalPaginas: number
  totalElementos: number
  alCambiar: (pagina: number) => void
}) {
  return (
    <nav className="paginador" aria-label="Paginación">
      <p>
        Página {numero + 1} de {Math.max(totalPaginas, 1)} · {cantidad(totalElementos, 'reserva', 'reservas')}
      </p>
      <div className="paginador-botones">
        <button
          type="button"
          className="boton boton-secundario"
          disabled={numero === 0}
          onClick={() => alCambiar(numero - 1)}
        >
          <span aria-hidden="true">←</span> Anterior
        </button>
        <button
          type="button"
          className="boton boton-secundario"
          disabled={numero + 1 >= totalPaginas}
          onClick={() => alCambiar(numero + 1)}
        >
          Siguiente <span aria-hidden="true">→</span>
        </button>
      </div>
    </nav>
  )
}
