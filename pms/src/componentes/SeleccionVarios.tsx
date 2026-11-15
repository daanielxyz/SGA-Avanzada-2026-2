import { useEffect, useId, useRef } from 'react'
import { textoDeEleccion, type Atajo } from './eleccion'

/**
 * Filtro de varios valores (DEC-119): un botón que dice lo elegido y abre una lista de casillas. Ninguna marcada es
 * «Todos». Se cierra con Escape, al tocar fuera o con «Listo».
 */
export default function SeleccionVarios({ rotulo, opciones, elegidos, atajos = [], alCambiar }: {
  rotulo: string
  opciones: readonly (readonly [string, string])[]
  elegidos: readonly string[]
  atajos?: readonly Atajo[]
  alCambiar: (elegidos: string[]) => void
}) {
  const id = useId()
  const caja = useRef<HTMLDetailsElement>(null)
  const texto = textoDeEleccion(elegidos, opciones, atajos)

  useEffect(() => {
    function fuera(evento: PointerEvent) {
      if (caja.current?.open && !caja.current.contains(evento.target as Node)) caja.current.open = false
    }
    document.addEventListener('pointerdown', fuera)
    return () => document.removeEventListener('pointerdown', fuera)
  }, [])

  const cerrar = () => {
    if (!caja.current) return
    caja.current.open = false
    caja.current.querySelector('summary')?.focus()
  }
  const alternar = (valor: string) => alCambiar(elegidos.includes(valor)
    ? elegidos.filter((v) => v !== valor)
    : opciones.map(([v]) => v).filter((v) => v === valor || elegidos.includes(v)))

  return (
    <div className="seleccion-varios-campo">
      <span className="campo-rotulo" id={`${id}-rotulo`}>{rotulo}</span>
      <details ref={caja} className="seleccion-varios"
        onKeyDown={(evento) => {
          if (evento.key === 'Escape' && caja.current?.open) {
            evento.preventDefault()
            cerrar()
          }
        }}>
        <summary className="campo-control seleccion-varios-boton" aria-labelledby={`${id}-rotulo ${id}-valor`}>
          <span id={`${id}-valor`} className="seleccion-varios-texto">{texto}</span>
        </summary>
        <div className="seleccion-varios-lista">
          {atajos.length > 0 && (
            <div className="seleccion-varios-atajos">
              {atajos.map((atajo) => (
                <button key={atajo.nombre} type="button" className="boton boton-secundario boton-chico"
                  onClick={() => alCambiar([...atajo.valores])}>
                  {atajo.nombre}
                </button>
              ))}
            </div>
          )}
          <fieldset>
            <legend className="solo-lectores">{rotulo}</legend>
            {opciones.map(([valor, nombre]) => (
              <label key={valor} className="casilla">
                <input type="checkbox" value={valor} checked={elegidos.includes(valor)} onChange={() => alternar(valor)} />
                {nombre}
              </label>
            ))}
          </fieldset>
          <div className="seleccion-varios-pie">
            <button type="button" className="enlace" disabled={elegidos.length === 0} onClick={() => alCambiar([])}>
              Todos
            </button>
            <button type="button" className="boton boton-chico" onClick={cerrar}>Listo</button>
          </div>
        </div>
      </details>
    </div>
  )
}
