import { useEffect, useId, useLayoutEffect, useRef, useState, type ComponentProps, type KeyboardEvent } from 'react'
import { FECHA_MAXIMA, fechaHoy } from '../formato'
import Calendario from './Calendario'
import { primeroDelMes } from './fechasCalendario'
import Icono from './Icono'

const ANCHO_PANEL = 312
const SEPARACION = 6
const BORDE = 8

/** Pone un valor en el campo como si se hubiera escrito: así el `onChange` de quien lo usa se entera igual. */
function escribir(campo: HTMLInputElement, valor: string) {
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set?.call(campo, valor)
  campo.dispatchEvent(new Event('input', { bubbles: true }))
}

const anio = (fecha: string) => Number(fecha.slice(0, 4))

/**
 * Un campo de fecha (o de fecha y hora) con el calendario del PMS en vez del del navegador (DEC-139, fase 2). Se usa
 * igual que un `<input type="date">`: las mismas propiedades y el mismo `onChange`. La fecha se puede seguir
 * escribiendo; el botón del calendario abre el de los bloqueos, flotando bajo el campo (o encima, si abajo no cabe), y
 * al elegir un día se cierra y el foco vuelve al campo. Con hora, elegir un día conserva la hora escrita.
 *
 * @param etiqueta el nombre del campo, para el botón: «Calendario: Fecha de nacimiento»
 */
export default function CampoFecha({ etiqueta, type = 'date', ...control }: {
  etiqueta: string
  type?: 'date' | 'datetime-local'
} & Omit<ComponentProps<'input'>, 'type'>) {
  const id = useId()
  const campo = useRef<HTMLInputElement>(null)
  const envoltura = useRef<HTMLDivElement>(null)
  const panel = useRef<HTMLDivElement>(null)
  const [abierto, setAbierto] = useState(false)
  const [posicion, setPosicion] = useState<{ top: number; left: number } | null>(null)

  const hoy = fechaHoy()
  const valor = String(control.value ?? '')
  const fecha = valor.slice(0, 10)
  const hora = type === 'datetime-local' ? valor.slice(11, 16) || '12:00' : ''
  const minimo = typeof control.min === 'string' && control.min ? control.min.slice(0, 10) : ''
  const maximo = typeof control.max === 'string' && control.max && control.max !== FECHA_MAXIMA ? control.max.slice(0, 10) : ''
  // Sin límites, de cien años atrás (una fecha de nacimiento) a diez adelante
  const primerMes = primeroDelMes(minimo || `${anio(hoy) - 100}-01-01`)
  const ultimoMes = primeroDelMes(maximo || `${anio(hoy) + 10}-12-01`)
  const [mes, setMes] = useState(primerMes)

  function abrir() {
    const base = fecha || (maximo && maximo < hoy ? maximo : minimo && minimo > hoy ? minimo : hoy)
    const inicial = primeroDelMes(base)
    setMes(inicial < primerMes ? primerMes : inicial > ultimoMes ? ultimoMes : inicial)
    setPosicion(null)
    setAbierto(true)
  }

  function cerrar() {
    setAbierto(false)
    campo.current?.focus()
  }

  // Flota bajo el campo; si abajo no cabe y arriba sí, encima. Se recoloca al desplazar la página o un diálogo
  useLayoutEffect(() => {
    if (!abierto) return
    const colocar = () => {
      const caja = envoltura.current?.getBoundingClientRect()
      const alto = panel.current?.offsetHeight ?? 0
      if (!caja) return
      const abajo = caja.bottom + SEPARACION
      const arriba = caja.top - SEPARACION - alto
      const top = abajo + alto > window.innerHeight - BORDE && arriba > BORDE ? arriba : abajo
      const left = Math.max(BORDE, Math.min(caja.left, window.innerWidth - ANCHO_PANEL - BORDE))
      setPosicion({ top, left })
    }
    colocar()
    window.addEventListener('resize', colocar)
    window.addEventListener('scroll', colocar, true)
    return () => {
      window.removeEventListener('resize', colocar)
      window.removeEventListener('scroll', colocar, true)
    }
  }, [abierto, mes])

  useEffect(() => {
    if (!abierto) return
    const alTocarAfuera = (evento: PointerEvent) => {
      if (!envoltura.current?.contains(evento.target as Node)) setAbierto(false)
    }
    document.addEventListener('pointerdown', alTocarAfuera)
    return () => document.removeEventListener('pointerdown', alTocarAfuera)
  }, [abierto])

  function alTeclear(evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key !== 'Escape' || !abierto) return
    // Dentro de un diálogo, Escape cierra solo el calendario: el diálogo ve la marca y no se cierra (Dialogo.tsx)
    evento.preventDefault()
    evento.stopPropagation()
    evento.currentTarget.closest('dialog')?.setAttribute('data-escape-atendido', '')
    cerrar()
  }

  function elegir(dia: string) {
    if (campo.current) escribir(campo.current, type === 'datetime-local' ? `${dia}T${hora}` : dia)
    cerrar()
  }

  return (
    // Escape se atiende en todo el campo: también si el foco quedó en el botón del calendario
    <div className="campo-fecha" ref={envoltura} onKeyDown={alTeclear}>
      <input {...control} ref={campo} type={type} />
      <button type="button" className="campo-fecha-boton" aria-label={`Calendario: ${etiqueta}`}
        aria-haspopup="dialog" aria-expanded={abierto} aria-controls={abierto ? `${id}-panel` : undefined}
        disabled={control.disabled || control.readOnly} onClick={() => (abierto ? cerrar() : abrir())}>
        <Icono nombre="reservas" />
      </button>
      {abierto && (
        <div id={`${id}-panel`} ref={panel} className="campo-fecha-panel" role="dialog" aria-label={`Elegir ${etiqueta.toLowerCase()}`}
          // Mientras se mide, invisible pero enfocable (con `visibility: hidden` el foco no entraría al día)
          style={posicion ?? { opacity: 0, top: 0, left: 0 }}>
          <Calendario
            etiqueta="Calendario"
            mes={mes}
            alCambiarMes={setMes}
            primerMes={primerMes}
            ultimoMes={ultimoMes}
            inicio={fecha}
            saltos
            compacto
            enfocarAlAbrir
            estadoDe={(dia) => {
              if (minimo && dia < minimo) return { elegible: false, texto: 'antes de lo permitido' }
              if (maximo && dia > maximo) return { elegible: false, texto: 'después de lo permitido' }
              return { elegible: true, texto: dia === fecha ? 'la elegida' : 'elegir' }
            }}
            alElegir={elegir}
          />
        </div>
      )}
    </div>
  )
}
