/**
 * Por qué un apartamento está fuera de servicio (DEC-113), para que recepción y el personal sepan por qué no se vende y
 * cuándo podría volver. Nada si no se dijo.
 *
 * @param corto en la celda angosta del tablero: dos renglones y el texto completo al pasar el puntero
 */
export default function MotivoFueraDeServicio({ motivo, corto = false }: { motivo?: string | null; corto?: boolean }) {
  if (!motivo?.trim()) return null
  return (
    <p className={corto ? 'motivo-fuera motivo-fuera-corto' : 'motivo-fuera'} title={corto ? motivo : undefined}>
      <span className="motivo-fuera-rotulo">Motivo:</span> {motivo}
    </p>
  )
}
