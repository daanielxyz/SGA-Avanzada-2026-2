import type { FormEvent, ReactNode } from 'react'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import type { ErrorMostrado } from '../reserva/mensajes'

/** El rechazo que se muestra arriba del diálogo, con qué no se hizo. */
export type Rechazo = ErrorMostrado & { asunto: string }

/**
 * Diálogo de una acción del apartamento: el rechazo arriba, el formulario en el cuerpo y, al pie, «Volver» y el botón
 * principal. Mientras se envía no se puede cerrar ni volver a enviar.
 */
export default function DialogoFormulario({ titulo, subtitulo, enviando, rechazo, textoBoton, textoEnviando,
  peligro = false, bloqueado = false, pie, alCerrar, alEnviar, children }: {
  titulo: string
  subtitulo?: ReactNode
  enviando: boolean
  rechazo: Rechazo | null
  textoBoton: string
  textoEnviando: string
  peligro?: boolean
  /** Con un rechazo que no se arregla aquí: solo queda volver. */
  bloqueado?: boolean
  /** Algo más bajo el rechazo, como un enlace para resolverlo. */
  pie?: ReactNode
  alCerrar: () => void
  alEnviar: (evento: FormEvent<HTMLFormElement>) => void
  children: ReactNode
}) {
  return (
    <Dialogo titulo={titulo} subtitulo={subtitulo} ocupado={enviando} alCerrar={alCerrar}>
      <form className="dialogo-contenido" onSubmit={alEnviar} noValidate>
        <div className="dialogo-cuerpo">
          {rechazo && (
            <Aviso tipo="error" asunto={rechazo.asunto} mensaje={rechazo.mensaje} detalles={rechazo.detalles}
              accion={pie} />
          )}
          {children}
          {enviando && <p className="solo-lectores" role="status">{textoEnviando}</p>}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" disabled={enviando} onClick={alCerrar}>
            Volver
          </button>
          <button type="submit" className={peligro ? 'boton boton-peligro' : 'boton'} disabled={enviando || bloqueado}>
            {enviando ? textoEnviando : textoBoton}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}

/** Un campo de texto largo o corto con su contador; más del máximo no se puede escribir. */
export function CampoConContador({ id, rotulo, nota, valor, maximo, error, pista, varias = false, focoInicial,
  deshabilitado, alCambiar }: {
  id: string
  rotulo: string
  nota?: string
  valor: string
  maximo: number
  error?: string
  pista?: string
  varias?: boolean
  focoInicial?: boolean
  deshabilitado?: boolean
  alCambiar: (valor: string) => void
}) {
  const descritoPor = [error && `${id}-error`, pista && !error && `${id}-pista`, `${id}-contador`]
    .filter(Boolean).join(' ')
  const comunes = {
    id,
    className: varias ? 'campo-control campo-texto' : 'campo-control',
    maxLength: maximo,
    value: valor,
    disabled: deshabilitado,
    'data-foco-inicial': focoInicial || undefined,
    'aria-invalid': Boolean(error),
    'aria-describedby': descritoPor,
  }
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>
        {rotulo} {nota && <span className="campo-rotulo-nota">({nota})</span>}
      </label>
      {varias
        ? <textarea {...comunes} rows={4} onChange={(e) => alCambiar(e.target.value)} />
        : <input {...comunes} type="text" autoComplete="off" onChange={(e) => alCambiar(e.target.value)} />}
      <div className="campo-contador">
        {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
        {!error && pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
        <p className="contador" id={`${id}-contador`}>
          {valor.length.toLocaleString('es-CO')} / {maximo.toLocaleString('es-CO')}
        </p>
      </div>
    </div>
  )
}
