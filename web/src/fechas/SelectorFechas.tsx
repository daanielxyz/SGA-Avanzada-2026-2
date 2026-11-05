import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import type { Estadia } from '../api/apartamentos'
import { fechaCorta } from '../formato'
import Calendario from './Calendario'

const ANCHA = '(min-width: 720px)'

/** Dos meses caben en pantallas anchas; en el celular, uno. */
function usePantallaAncha() {
  const [ancha, setAncha] = useState(() => window.matchMedia?.(ANCHA).matches ?? true)
  useEffect(() => {
    const consulta = window.matchMedia?.(ANCHA)
    if (!consulta) return
    const alCambiar = () => setAncha(consulta.matches)
    consulta.addEventListener('change', alCambiar)
    return () => consulta.removeEventListener('change', alCambiar)
  }, [])
  return ancha
}

interface Props {
  estadia: Estadia
  alCambiar: (estadia: Estadia) => void
  /** Con un apartamento, el calendario tacha sus noches ocupadas. */
  codigo?: string
  /** Hacia qué lado crece el calendario en pantallas anchas: «fin» cuando el campo está contra el borde derecho. */
  alinear?: 'inicio' | 'fin'
}

/**
 * Llegada y salida: dos botones que abren el mismo calendario, con limpiar y cerrar. Se cierra también con Escape o
 * tocando fuera, y el foco vuelve al botón que lo abrió.
 */
export default function SelectorFechas({ estadia, alCambiar, codigo, alinear = 'inicio' }: Props) {
  const id = useId()
  const ancha = usePantallaAncha()
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)
  const origen = useRef<HTMLButtonElement | null>(null)

  useEffect(() => {
    if (!abierto) return
    const alTocarAfuera = (evento: PointerEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false)
    }
    document.addEventListener('pointerdown', alTocarAfuera)
    return () => document.removeEventListener('pointerdown', alTocarAfuera)
  }, [abierto])

  function cerrar() {
    setAbierto(false)
    origen.current?.focus()
  }

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'Escape') {
      evento.stopPropagation()
      cerrar()
    }
  }

  const disparador = (rotulo: string, fecha: string) => (
    <div className="campo">
      <span className="rotulo-campo" id={`${id}-${rotulo}`}>{rotulo}</span>
      <button type="button" className="fecha-disparador" aria-expanded={abierto} aria-controls={`${id}-panel`}
        aria-labelledby={`${id}-${rotulo} ${id}-${rotulo}-valor`}
        onClick={(e) => {
          origen.current = e.currentTarget
          setAbierto((a) => !a)
        }}>
        <span id={`${id}-${rotulo}-valor`} className={fecha ? undefined : 'suave'}>
          {fecha ? fechaCorta(fecha) : 'Agregar fecha'}
        </span>
        <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor"
          strokeWidth="1.6" strokeLinecap="round">
          <rect x="3" y="4.5" width="14" height="12.5" rx="2" />
          <path d="M3 8.5h14M7 2.5v4M13 2.5v4" />
        </svg>
      </button>
    </div>
  )

  return (
    <div className="selector-fechas" ref={contenedor}>
      {disparador('Llegada', estadia.entrada)}
      {disparador('Salida', estadia.salida)}
      {abierto && (
        <div id={`${id}-panel`} className="fechas-panel" data-alinear={alinear} role="dialog"
          aria-label="Elegir fechas" onKeyDown={alTeclear}>
          <Calendario codigo={codigo} estadia={estadia} alCambiar={alCambiar} mesesVisibles={ancha ? 2 : 1} />
          <div className="fechas-acciones">
            <button type="button" className="secundario" disabled={!estadia.entrada && !estadia.salida}
              onClick={() => alCambiar({ entrada: '', salida: '' })}>
              Limpiar fechas
            </button>
            <button type="button" className="boton-cerrar" onClick={cerrar}>Cerrar</button>
          </div>
        </div>
      )}
    </div>
  )
}
