import { useEffect, useId, useRef, type ReactNode } from 'react'

/**
 * Ventana modal de la base. Usa el `dialog` del navegador, que deja el foco dentro; al cerrarla vuelve al botón que la
 * abrió. Dentro van un elemento con `dialogo-cuerpo` y otro con `dialogo-pie`. Si un control tiene `data-foco-inicial`, el
 * foco empieza ahí; si no, en el título. También se mueve cuando cambia el título, porque el contenido ya es otro.
 *
 * @param ocupado mientras se envía algo no se puede cerrar
 * @param ancho  para una tabla: en el computador mide 720 px en vez de 480
 */
export default function Dialogo({ titulo, subtitulo, ocupado = false, ancho = false, alCerrar, children }: {
  titulo: string
  subtitulo?: ReactNode
  ocupado?: boolean
  ancho?: boolean
  alCerrar: () => void
  children: ReactNode
}) {
  const marco = useRef<HTMLDialogElement>(null)
  const encabezado = useRef<HTMLHeadingElement>(null)
  const id = useId()

  useEffect(() => {
    const ventana = marco.current
    const abrio = document.activeElement
    ventana?.showModal()
    return () => {
      ventana?.close()
      // React ya la sacó de la página, así que el navegador no devuelve el foco por su cuenta
      if (abrio instanceof HTMLElement && abrio.isConnected) abrio.focus()
    }
  }, [])

  useEffect(() => {
    const inicial = marco.current?.querySelector<HTMLElement>('[data-foco-inicial]')
    ;(inicial ?? encabezado.current)?.focus()
  }, [titulo])

  return (
    <dialog
      ref={marco}
      className={ancho ? 'dialogo dialogo-ancho' : 'dialogo'}
      aria-labelledby={id}
      onCancel={(evento) => {
        evento.preventDefault()
        if (!ocupado) alCerrar()
      }}
    >
      <header className="dialogo-cabecera">
        <div className="dialogo-titulos">
          <h2 id={id} ref={encabezado} tabIndex={-1}>
            {titulo}
          </h2>
          {subtitulo && <p className="dialogo-subtitulo">{subtitulo}</p>}
        </div>
        <button
          type="button"
          className="boton boton-secundario boton-icono"
          aria-label="Cerrar"
          disabled={ocupado}
          onClick={alCerrar}
        >
          <span aria-hidden="true">✕</span>
        </button>
      </header>
      {children}
    </dialog>
  )
}
