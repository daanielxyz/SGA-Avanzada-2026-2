const TIPOS = {
  error: { titulo: 'Error', sello: '!', rol: 'alert' },
  info: { titulo: 'Información', sello: 'i', rol: 'status' },
  advertencia: { titulo: 'Advertencia', sello: '!', rol: 'status' },
} as const

/**
 * Mensaje junto a lo que lo causó («Terminado», docs/SPRINTS.md). El tipo también se dice con palabras, no solo con
 * el color.
 *
 * @param detalles los errores por campo de un 400 (`ErrorResponse.detalles`, DEC-33)
 */
export default function Aviso({ tipo, mensaje, detalles = [] }: {
  tipo: keyof typeof TIPOS
  mensaje: string
  detalles?: string[]
}) {
  const { titulo, sello, rol } = TIPOS[tipo]
  return (
    <div className={`aviso aviso-${tipo}`} role={rol}>
      <span className="aviso-sello" aria-hidden="true">
        {sello}
      </span>
      <div>
        <strong>{titulo}</strong>
        <p>{mensaje}</p>
        {detalles.length > 0 && (
          <ul>
            {detalles.map((detalle) => (
              <li key={detalle}>{detalle}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
