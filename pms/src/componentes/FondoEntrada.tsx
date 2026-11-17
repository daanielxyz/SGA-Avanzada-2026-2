import { useEffect, useRef, useState } from 'react'
import Logo from './Logo'

/**
 * Dónde están el video y su póster: en `public/video/`, en git y publicados con el PMS (4 MB, sin audio). Si algún día
 * se sirven desde otro lugar, `VITE_VIDEO_ENTRADA` y `VITE_POSTER_ENTRADA` apuntan allí al construir.
 */
const VIDEO: string = import.meta.env.VITE_VIDEO_ENTRADA ?? '/video/entrada.mp4'
const POSTER: string = import.meta.env.VITE_POSTER_ENTRADA ?? '/video/entrada.jpg'

/** El mismo ancho desde el que la entrada muestra su fondo (`src/index.css`): por debajo, el celular no lo ve. */
const ANCHA = '(min-width: 800px)'

/** Si la pantalla cumple una consulta de CSS, al día con sus cambios. Sin `matchMedia`, no. */
function useConsulta(consulta: string): boolean {
  const [cumple, setCumple] = useState(() => typeof window.matchMedia === 'function' && window.matchMedia(consulta).matches)
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return
    const lista = window.matchMedia(consulta)
    const alCambiar = () => setCumple(lista.matches)
    alCambiar()
    lista.addEventListener('change', alCambiar)
    return () => lista.removeEventListener('change', alCambiar)
  }, [consulta])
  return cumple
}

/**
 * El fondo de las pantallas sin sesión (entrar, activar la cuenta, recuperar la contraseña): un video que cubre la
 * pantalla, con el logo arriba. Es decoración: los lectores de pantalla no lo anuncian.
 *
 * - En el celular no se ve, así que ni el video ni el póster se piden.
 * - Se reproduce siempre, en silencio y en bucle, también con «reducir movimiento», y no tiene botón de pausa: lo
 *   decidió el equipo (DEC-139), sabiendo que WCAG 2.2.2 pide poder detener lo que se mueve solo más de 5 segundos.
 * - Si el video o el póster no están (un clon sin esos archivos), queda el degradado de siempre.
 */
export default function FondoEntrada() {
  const ancha = useConsulta(ANCHA)
  const video = useRef<HTMLVideoElement>(null)
  const [conPoster, setConPoster] = useState(true)
  const [estado, setEstado] = useState<'cargando' | 'listo' | 'falla'>('cargando')
  const conVideo = ancha && estado !== 'falla'

  // Por si el navegador no lo arranca solo con `autoplay`: en silencio siempre se puede
  useEffect(() => {
    const elemento = video.current
    if (!conVideo || !elemento) return
    elemento.muted = true
    void elemento.play()?.catch(() => undefined)
  }, [conVideo])

  return (
    <div className="entrada-arte">
      {ancha && conPoster && (
        <img className="entrada-fondo" src={POSTER} alt="" onError={() => setConPoster(false)} />
      )}
      {conVideo && (
        <video
          ref={video}
          className={estado === 'listo' ? 'entrada-fondo entrada-video listo' : 'entrada-fondo entrada-video'}
          src={VIDEO}
          autoPlay
          muted
          loop
          playsInline
          preload="auto"
          disablePictureInPicture
          aria-hidden="true"
          tabIndex={-1}
          onPlaying={() => setEstado('listo')}
          onError={() => setEstado('falla')}
        />
      )}
      <Logo decorativo />
    </div>
  )
}
