import { useId, type ComponentProps, type ReactNode } from 'react'

/** Un campo con su etiqueta, su pista y su error, conectados para los lectores de pantalla. */
export default function Campo({ etiqueta, nota, pista, error, opciones, ...control }: {
  etiqueta: string
  nota?: string
  pista?: ReactNode
  error?: string
  /** Con opciones se dibuja una lista: `[valor, texto]`. */
  opciones?: readonly (readonly [string, string])[]
} & ComponentProps<'input'>) {
  const id = useId()
  const descripcion = error ? `${id}-error` : pista ? `${id}-pista` : undefined
  const comunes = { id, 'aria-invalid': Boolean(error), 'aria-describedby': descripcion, className: 'campo-control' }
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>
        {etiqueta} {nota && <span className="campo-rotulo-nota">({nota})</span>}
      </label>
      {opciones ? (
        <select
          {...comunes}
          value={control.value}
          disabled={control.disabled}
          onChange={control.onChange as unknown as ComponentProps<'select'>['onChange']}
        >
          {opciones.map(([valor, texto]) => (
            <option key={valor} value={valor}>
              {texto}
            </option>
          ))}
        </select>
      ) : (
        <input {...control} {...comunes} />
      )}
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
      {!error && pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
    </div>
  )
}
