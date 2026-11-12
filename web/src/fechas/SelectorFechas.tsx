import { useEffect, useId, useImperativeHandle, useRef, useState, type KeyboardEvent, type Ref } from 'react'
import type { Estadia } from '../api/apartamentos'
import { diaDeLaSemana, diaYMes } from '../formato'
import Calendario from './Calendario'

const ANCHA = '(min-width: 720px)'
const MARGEN_ABAJO = 16
const DURACION_ENTRADA_MS = 160 // la de .fechas-panel en index.css; si no hay animación, se ajusta igual

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

/** Para abrir el calendario desde otro botón de la página, como el de reservar sin fechas. */
export interface ControlFechas {
  abrir: () => void
}

interface Props {
  ref?: Ref<ControlFechas>
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
export default function SelectorFechas({ ref, estadia, alCambiar, codigo, alinear = 'inicio' }: Props) {
  const id = useId()
  const ancha = usePantallaAncha()
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)
  const origen = useRef<HTMLButtonElement | null>(null)
  const llegada = useRef<HTMLButtonElement | null>(null)
  const panel = useRef<HTMLDivElement>(null)

  useImperativeHandle(ref, () => ({
    abrir: () => {
      origen.current = llegada.current
      setAbierto(true)
    },
  }), [])

  // Que se vea entero sin buscarlo: la página baja lo justo, con un respiro abajo. Se espera a que termine la
  // animación de entrada, porque mientras dura el panel es más pequeño y la página aún no alcanza a bajar
  useEffect(() => {
    const elemento = panel.current
    if (!abierto || !elemento) return
    const ajustar = () => {
      const falta = elemento.getBoundingClientRect().bottom + MARGEN_ABAJO - window.innerHeight
      if (falta <= 0) return
      const quieto = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
      window.scrollBy?.({ top: falta, behavior: quieto ? 'auto' : 'smooth' })
    }
    elemento.addEventListener('animationend', ajustar, { once: true })
    const sinAnimacion = window.setTimeout(ajustar, DURACION_ENTRADA_MS + 40)
    return () => {
      elemento.removeEventListener('animationend', ajustar)
      window.clearTimeout(sinAnimacion)
    }
  }, [abierto])

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
        ref={rotulo === 'Llegada' ? llegada : undefined}
        aria-labelledby={`${id}-${rotulo} ${id}-${rotulo}-valor`}
        onClick={(e) => {
          origen.current = e.currentTarget
          setAbierto((a) => !a)
        }}>
        <span id={`${id}-${rotulo}-valor`} className={fecha ? undefined : 'suave'}>
          {/* En una casilla angosta el día de la semana se oculta y la fecha sigue cabiendo (index.css) */}
          {fecha ? <><span className="dia-semana">{diaDeLaSemana(fecha)}, </span>{diaYMes(fecha)}</> : 'Agregar fecha'}
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
        <div id={`${id}-panel`} ref={panel} className="fechas-panel" data-alinear={alinear} role="dialog"
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
