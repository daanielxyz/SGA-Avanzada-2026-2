import type { CorreccionDocumento } from '../api/reservas'
import { fechaYHora } from './llegada/llegada'

const documento = (tipo: string, numero: string) => `${tipo === 'PASAPORTE' ? 'Pasaporte' : tipo} ${numero}`

/**
 * Los documentos corregidos de la reserva (DEC-120), del más reciente al más antiguo: de quién, cómo estaba y cómo
 * quedó, por qué, quién lo hizo y cuándo. Las del titular llevan su marca. Sin correcciones no se muestra.
 */
export default function CorreccionesDocumento({ correcciones }: { correcciones: CorreccionDocumento[] | null | undefined }) {
  if (!correcciones || correcciones.length === 0) return null
  const recientes = [...correcciones].reverse()
  return (
    <section aria-labelledby="titulo-correcciones" className="detalle-seccion">
      <h2 id="titulo-correcciones">Documentos corregidos ({correcciones.length})</h2>
      <ol className="tarjeta correcciones">
        {recientes.map((correccion, i) => (
          <li key={`${correccion.fechaHora}-${i}`} className="correccion">
            <p className="correccion-quien">
              <strong className="texto-largo">{correccion.ocupante}</strong>
              {correccion.titular && <span className="etiqueta">Titular</span>}
            </p>
            <p className="correccion-cambio">
              <span className="codigo tachado-suave">
                <span className="solo-lectores">Antes: </span>{documento(correccion.tipoAnterior, correccion.numeroAnterior)}
              </span>
              <span aria-hidden="true"> → </span>
              <span className="codigo">
                <span className="solo-lectores">, ahora: </span>{documento(correccion.tipoNuevo, correccion.numeroNuevo)}
              </span>
            </p>
            <p className="correccion-motivo texto-largo">{correccion.motivo}</p>
            <p className="campo-pista">
              {correccion.autorNombre ? `Por ${correccion.autorNombre}, el ` : 'El '}{fechaYHora(correccion.fechaHora)}
            </p>
          </li>
        ))}
      </ol>
    </section>
  )
}
