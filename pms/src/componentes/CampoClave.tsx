import { useId, useState, type Ref } from 'react'

/**
 * Un campo de contraseña con «Mostrar», el mismo de «Iniciar sesión». La pista (la regla) se ve antes de escribir y el
 * error va debajo, sin taparla.
 *
 * @param autocompletar `current-password` para la actual y `new-password` para la nueva: así el navegador ofrece
 *                      guardarla o proponer una
 */
export default function CampoClave({ etiqueta, valor, error, pista, autocompletar, deshabilitado, focoInicial,
  referencia, alCambiar }: {
  etiqueta: string
  valor: string
  error?: string
  pista?: string
  autocompletar: 'current-password' | 'new-password'
  deshabilitado?: boolean
  focoInicial?: boolean
  referencia?: Ref<HTMLInputElement>
  alCambiar: (valor: string) => void
}) {
  const id = useId()
  const [ver, setVer] = useState(false)
  const descritoPor = [error && `${id}-error`, pista && `${id}-pista`].filter(Boolean).join(' ') || undefined
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>{etiqueta}</label>
      <div className="campo-clave">
        <input
          ref={referencia}
          id={id}
          className="campo-control"
          type={ver ? 'text' : 'password'}
          autoComplete={autocompletar}
          value={valor}
          disabled={deshabilitado}
          data-foco-inicial={focoInicial || undefined}
          aria-invalid={Boolean(error)}
          aria-describedby={descritoPor}
          onChange={(e) => alCambiar(e.target.value)}
        />
        <button
          type="button"
          className="ver-clave"
          aria-label={`${ver ? 'Ocultar' : 'Mostrar'} ${etiqueta.toLowerCase()}`}
          disabled={deshabilitado}
          onClick={() => setVer(!ver)}
        >
          {ver ? 'Ocultar' : 'Mostrar'}
        </button>
      </div>
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
      {pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
    </div>
  )
}
