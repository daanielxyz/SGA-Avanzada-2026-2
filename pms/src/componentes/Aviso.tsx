import type { ReactNode } from 'react'

const TIPOS = {
  error: { titulo: 'Error', sello: '!', rol: 'alert' },
  info: { titulo: 'Información', sello: 'i', rol: 'status' },
  advertencia: { titulo: 'Advertencia', sello: '!', rol: 'status' },
} as const

/**
 * Mensaje junto a lo que lo causó («Terminado», docs/SPRINTS.md). El tipo también se dice con palabras, no solo con
 * el color.
 *
 * @param asunto   qué fue lo que salió mal, cuando el mensaje solo no lo dice: «No se pudo aplicar el filtro»
 * @param detalles los errores por campo de un 400 (`ErrorResponse.detalles`, DEC-33)
 * @param accion   lo que se puede hacer ahí mismo, como reintentar
 */
export default function Aviso({ tipo, asunto, mensaje, detalles = [], accion }: {
  tipo: keyof typeof TIPOS
  asunto?: string
  mensaje: string
  detalles?: string[]
  accion?: ReactNode
}) {
  const { titulo, sello, rol } = TIPOS[tipo]
  return (
    <div className={`aviso aviso-${tipo}`} role={rol}>
      <span className="aviso-sello" aria-hidden="true">
        {sello}
      </span>
      <div className="aviso-cuerpo">
        <strong>{asunto ? `${titulo} · ${asunto}` : titulo}</strong>
        <p>{mensaje}</p>
        {detalles.length > 0 && (
          <ul>
            {detalles.map((detalle) => (
              <li key={detalle}>{detalle}</li>
            ))}
          </ul>
        )}
      </div>
      {accion}
    </div>
  )
}
