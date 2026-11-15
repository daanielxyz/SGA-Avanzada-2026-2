import Icono from '../componentes/Icono'
import { useAsistente } from './contexto'

export const ID_BOTON_AYUDA = 'boton-ayuda'
export const ID_PANEL_AYUDA = 'panel-ayuda'

/** «Ayuda» en la cabecera, junto a «Cerrar sesión» (DEC-127). En el celular, solo el ícono. */
export default function BotonAyuda() {
  const asistente = useAsistente()
  if (!asistente) return null
  const { abierto, abrir, cerrar } = asistente
  return (
    <button
      id={ID_BOTON_AYUDA}
      type="button"
      className="boton boton-secundario boton-chico boton-ayuda"
      aria-expanded={abierto}
      aria-controls={ID_PANEL_AYUDA}
      onClick={abierto ? cerrar : abrir}
    >
      <Icono nombre="ayuda" />
      <span className="boton-ayuda-texto">Ayuda</span>
    </button>
  )
}
