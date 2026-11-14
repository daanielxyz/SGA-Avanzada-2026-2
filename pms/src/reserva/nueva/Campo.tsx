import { useId, type ComponentProps, type ReactNode } from 'react'
import { FECHA_MAXIMA } from '../../formato'

/**
 * Un campo con su etiqueta, su pista y su error, conectados para los lectores de pantalla. Con `contador`, cuenta lo
 * escrito contra su `maxLength`, a la derecha del error o la pista (como `CampoConContador` de Apartamentos).
 */
export default function Campo({ etiqueta, nota, marca, pista, error, invalido = false, opciones, contador = false, ...control }: {
  etiqueta: string
  nota?: string
  /** Algo más junto al rótulo, como la marca de un valor propuesto. */
  marca?: ReactNode
  pista?: ReactNode
  error?: string
  /** Inválido por un error que se muestra en otro lado, por ejemplo el de su grupo. */
  invalido?: boolean
  /** Con opciones se dibuja una lista: `[valor, texto]`. */
  opciones?: readonly (readonly [string, string])[]
  contador?: boolean
} & ComponentProps<'input'>) {
  const id = useId()
  // Escrita a mano, una fecha admite años de 5 o 6 cifras («20278»): con un máximo, el navegador deja solo 4
  if (control.type === 'date' && control.max === undefined) control.max = FECHA_MAXIMA
  const conContador = contador && control.maxLength !== undefined
  const descripcion = [error ? `${id}-error` : pista ? `${id}-pista` : null, conContador ? `${id}-contador` : null]
    .filter(Boolean).join(' ') || undefined
  const comunes = { id, 'aria-invalid': Boolean(error) || invalido, 'aria-describedby': descripcion, className: 'campo-control' }
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>
        {etiqueta} {nota && <span className="campo-rotulo-nota">({nota})</span>}
        {marca}
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
      {conContador ? (
        <div className="campo-contador">
          {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
          {!error && pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
          <p className="contador" id={`${id}-contador`}>
            {String(control.value ?? '').length.toLocaleString('es-CO')} / {control.maxLength?.toLocaleString('es-CO')}
          </p>
        </div>
      ) : (
        <>
          {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
          {!error && pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
        </>
      )}
    </div>
  )
}
