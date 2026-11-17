import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import Calendario from '../../componentes/Calendario'
import { desdeTexto, mesesEntre, primeroDelMes } from '../../componentes/fechasCalendario'
import Icono from '../../componentes/Icono'
import { cantidad, fechaCorta, fechaHoy, noches } from '../../formato'

// Recepción reserva con más anticipación que la web (que llega a un año): dos años
const MESES_HACIA_ADELANTE = 24
/** El mismo ancho desde el que el PMS deja de ser de celular (`src/index.css`): dos meses lado a lado. */
const ANCHA = '(min-width: 800px)'
// El respiro bajo el calendario al abrirlo: lo que mide el pie fijo del paso («Siguiente») más un poco de aire
const MARGEN_ABAJO = 104

export interface Estancia {
  /** Vacía mientras no se elige. */
  entrada: string
  /** Exclusiva: esa noche no se ocupa ni se cobra (EST-01). Vacía mientras no se elige. */
  salida: string
}

const diaCorto = new Intl.DateTimeFormat('es-CO', { weekday: 'short', timeZone: 'UTC' })

function usePantallaAncha(): boolean {
  const [ancha, setAncha] = useState(() => (typeof window.matchMedia === 'function' ? window.matchMedia(ANCHA).matches : true))
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return
    const consulta = window.matchMedia(ANCHA)
    const alCambiar = () => setAncha(consulta.matches)
    consulta.addEventListener('change', alCambiar)
    return () => consulta.removeEventListener('change', alCambiar)
  }, [])
  return ancha
}

/**
 * Entrada y salida de una reserva nueva: dos botones con aspecto de campo que abren el mismo calendario, el de la
 * del PMS, el mismo de los bloqueos (dos meses en el computador, uno en el celular). Un clic elige la entrada y el
 * siguiente la salida; al elegir la salida se cierra. También se cierra con Escape o tocando fuera, y el foco vuelve
 * al botón que lo abrió. Aquí todavía no hay apartamento, así que ninguna noche se tacha: la disponibilidad se ve en
 * el paso siguiente.
 */
export default function SelectorEstancia({ estancia, errores, alCambiar }: {
  estancia: Estancia
  errores: { entrada?: string; salida?: string }
  alCambiar: (estancia: Estancia) => void
}) {
  const id = useId()
  const ancha = usePantallaAncha()
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)
  const origen = useRef<HTMLButtonElement | null>(null)
  const botonSalida = useRef<HTMLButtonElement | null>(null)
  const panel = useRef<HTMLDivElement>(null)

  // Que se vea entero sin buscarlo: la página baja lo justo
  useEffect(() => {
    const elemento = panel.current
    if (!abierto || !elemento) return
    const falta = elemento.getBoundingClientRect().bottom + MARGEN_ABAJO - window.innerHeight
    if (falta > 0) window.scrollBy?.({ top: falta, behavior: 'smooth' })
  }, [abierto])

  useEffect(() => {
    if (!abierto) return
    const alTocarAfuera = (evento: PointerEvent) => {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false)
    }
    document.addEventListener('pointerdown', alTocarAfuera)
    return () => document.removeEventListener('pointerdown', alTocarAfuera)
  }, [abierto])

  function cerrar(destino: HTMLButtonElement | null = origen.current) {
    setAbierto(false)
    destino?.focus()
  }

  function alElegir(nueva: Estancia) {
    alCambiar(nueva)
    // Con las dos fechas ya no hay nada más que elegir aquí
    if (nueva.entrada && nueva.salida) cerrar(botonSalida.current)
  }

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === 'Escape') {
      evento.stopPropagation()
      cerrar()
    }
  }

  const disparador = (campo: 'entrada' | 'salida', rotulo: string, pista?: string) => {
    const fecha = estancia[campo]
    const error = errores[campo]
    const idCampo = `${id}-${campo}`
    return (
      <div className="campo">
        <span className="campo-rotulo" id={`${idCampo}-rotulo`}>{rotulo}</span>
        <button
          type="button"
          ref={campo === 'salida' ? botonSalida : undefined}
          className="campo-control fecha-disparador"
          aria-haspopup="dialog"
          aria-expanded={abierto}
          aria-controls={abierto ? `${id}-panel` : undefined}
          aria-labelledby={`${idCampo}-rotulo ${idCampo}-valor`}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${idCampo}-error` : pista ? `${idCampo}-pista` : undefined}
          onClick={(evento) => {
            origen.current = evento.currentTarget
            setAbierto((estaba) => !estaba)
          }}
        >
          <span id={`${idCampo}-valor`} className={fecha ? 'fecha-valor' : 'fecha-valor fecha-vacia'}>
            {fecha ? `${diaCorto.format(desdeTexto(fecha)).replace('.', '')}, ${fechaCorta(fecha)}` : 'Elegir fecha'}
          </span>
          <Icono nombre="reservas" />
        </button>
        {error && <p className="campo-mensaje" id={`${idCampo}-error`}>{error}</p>}
        {!error && pista && <p className="campo-pista" id={`${idCampo}-pista`}>{pista}</p>}
      </div>
    )
  }

  return (
    <div className="selector-estancia" ref={contenedor}>
      {disparador('entrada', 'Entrada')}
      {disparador('salida', 'Salida', 'La noche de salida no se cobra.')}
      {abierto && (
        <div id={`${id}-panel`} ref={panel} className="fechas-panel" role="dialog" aria-label="Elegir fechas"
          onKeyDown={alTeclear}>
          <CalendarioEstancia estancia={estancia} alCambiar={alElegir} mesesVisibles={ancha ? 2 : 1} acciones={(
            <div className="fechas-acciones">
              <button type="button" className="boton boton-secundario boton-chico"
                disabled={!estancia.entrada && !estancia.salida} onClick={() => alCambiar({ entrada: '', salida: '' })}>
                Limpiar fechas
              </button>
              <button type="button" className="boton boton-chico" onClick={() => cerrar()}>Listo</button>
            </div>
          )} />
        </div>
      )}
    </div>
  )
}

/** El calendario del PMS (el de los bloqueos) eligiendo una estancia: un clic la entrada, el siguiente la salida. */
function CalendarioEstancia({ estancia, alCambiar, mesesVisibles, acciones }: {
  estancia: Estancia
  alCambiar: (estancia: Estancia) => void
  mesesVisibles: number
  acciones: ReactNode
}) {
  const hoy = fechaHoy()
  const { entrada, salida } = estancia
  const ultimoMes = primeroDelMes(hoy, MESES_HACIA_ADELANTE - 1)
  const [mes, setMes] = useState(() => primeroDelMes(hoy,
    Math.min(Math.max(entrada ? mesesEntre(hoy, entrada) : 0, 0), MESES_HACIA_ADELANTE - mesesVisibles)))
  const eligiendoSalida = entrada !== '' && salida === ''

  return (
    <Calendario
      etiqueta="Calendario de la estancia"
      mes={mes}
      alCambiarMes={setMes}
      primerMes={primeroDelMes(hoy)}
      ultimoMes={ultimoMes}
      mesesVisibles={mesesVisibles}
      inicio={entrada}
      fin={salida}
      enfocarAlAbrir
      estadoDe={(fecha) => {
        if (fecha < hoy) return { elegible: false, texto: 'ya pasó' }
        if (fecha === entrada) return { elegible: true, texto: 'entrada' }
        if (fecha === salida) return { elegible: true, texto: 'salida' }
        return { elegible: true, texto: eligiendoSalida && fecha > entrada ? 'elegir como salida' : 'elegir como entrada' }
      }}
      // Con la entrada puesta, un día posterior es la salida; cualquier otro empieza de nuevo
      alElegir={(fecha) => alCambiar(eligiendoSalida && fecha > entrada ? { entrada, salida: fecha } : { entrada: fecha, salida: '' })}
      ayuda={ayuda(estancia)}
      leyenda={acciones}
    />
  )
}

function ayuda({ entrada, salida }: Estancia) {
  if (entrada && salida) {
    return `${fechaCorta(entrada)} → ${fechaCorta(salida)} · ${cantidad(noches(entrada, salida), 'noche', 'noches')}`
  }
  if (entrada) return `Entrada el ${fechaCorta(entrada)}. Ahora elige la salida.`
  return 'Elige la entrada.'
}
