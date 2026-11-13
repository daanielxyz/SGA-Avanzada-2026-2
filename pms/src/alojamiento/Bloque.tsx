import { useId, type ComponentProps, type FormEvent, type ReactNode } from 'react'
import Aviso from '../componentes/Aviso'
import type { Estado, Rechazo } from './useBloque'

/**
 * Tarjeta de un bloque: título, contenido y un pie con el aviso (guardado o cambios sin guardar) a la izquierda y el
 * botón a la derecha. El aviso lleva palabra y sello, no solo color.
 */
export function Bloque({ titulo, descripcion, estado, sucio, rechazo, guardado, boton, alEnviar, children }: {
  titulo: string
  descripcion?: ReactNode
  estado: Estado
  sucio: boolean
  rechazo: Rechazo | null
  /** «Datos guardados». */
  guardado: string
  /** «Guardar datos». */
  boton: string
  alEnviar: (evento: FormEvent) => void
  children: ReactNode
}) {
  const id = useId()
  return (
    <form className="tarjeta bloque" aria-labelledby={id} onSubmit={alEnviar} noValidate>
      <div className="bloque-cuerpo">
        <header className="bloque-cabecera">
          <h2 id={id}>{titulo}</h2>
          {descripcion && <p className="suave">{descripcion}</p>}
        </header>
        {rechazo && <Aviso tipo="error" asunto={rechazo.asunto} mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}
        <fieldset className="bloque-campos" disabled={estado === 'guardando'}>
          <legend className="solo-lectores">{titulo}</legend>
          {children}
        </fieldset>
      </div>
      <footer className="bloque-pie">
        <p className="bloque-estado" role="status">
          {estado === 'guardado' && !sucio && <><span className="bloque-sello bloque-sello-guardado" aria-hidden="true">✓</span>{guardado}</>}
          {sucio && estado !== 'guardando' && <><span className="bloque-sello bloque-sello-sucio" aria-hidden="true" />Cambios sin guardar</>}
        </p>
        <button type="submit" className="boton" disabled={estado === 'guardando'}>
          {estado === 'guardando' ? 'Guardando…' : boton}
        </button>
      </footer>
    </form>
  )
}

/**
 * Campo de los bloques: texto, número con unidad («horas», «%», «$») o texto largo, con su pista, su error y, si tiene
 * máximo, el contador.
 */
export function CampoBloque({ etiqueta, nota, pista, error, antes, despues, largo, maximo, valor, alCambiar, ...control }: {
  etiqueta: string
  nota?: string
  pista?: ReactNode
  error?: string
  /** Lo que va pegado antes del valor, como «$». */
  antes?: string
  /** La unidad, como «horas». */
  despues?: string
  /** Texto largo, en varias líneas. */
  largo?: boolean
  maximo?: number
  valor: string
  alCambiar: (valor: string) => void
} & Omit<ComponentProps<'input'>, 'value' | 'onChange'>) {
  const id = useId()
  const descritoPor = [error ? `${id}-error` : pista ? `${id}-pista` : null, maximo ? `${id}-contador` : null]
    .filter(Boolean).join(' ') || undefined
  const comunes = {
    id,
    value: valor,
    'aria-invalid': Boolean(error),
    'aria-describedby': descritoPor,
  }
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>
        {etiqueta} {nota && <span className="campo-rotulo-nota">({nota})</span>}
      </label>
      {largo ? (
        <textarea {...comunes} className="campo-control campo-texto" rows={4} onChange={(evento) => alCambiar(evento.target.value)} />
      ) : (
        <div className={antes || despues ? 'campo-unidad' : undefined}>
          {antes && <span className="campo-unidad-antes" aria-hidden="true">{antes}</span>}
          <input {...control} {...comunes} className="campo-control" onChange={(evento) => alCambiar(evento.target.value)} />
          {despues && <span className="campo-unidad-despues">{despues}</span>}
        </div>
      )}
      {(error || maximo) && (
        <div className="campo-contador">
          {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
          {maximo && <p className="contador" id={`${id}-contador`}>{valor.length} / {maximo}</p>}
        </div>
      )}
      {!error && pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
    </div>
  )
}

/** Dos o más opciones excluyentes («Por noche» o «Por estadía»), con su pregunta como leyenda. */
export function Opciones<V extends string>({ pregunta, pista, valor, opciones, alCambiar }: {
  pregunta: string
  pista?: ReactNode
  valor: V
  opciones: readonly (readonly [V, string])[]
  alCambiar: (valor: V) => void
}) {
  const nombre = useId()
  return (
    <fieldset className="campo opciones" aria-describedby={pista ? `${nombre}-pista` : undefined}>
      <legend className="campo-rotulo">{pregunta}</legend>
      <div className="opciones-lista">
        {opciones.map(([clave, texto]) => (
          <label key={clave} className="casilla">
            <input type="radio" name={nombre} value={clave} checked={valor === clave} onChange={() => alCambiar(clave)} />
            {texto}
          </label>
        ))}
      </div>
      {pista && <p className="campo-pista" id={`${nombre}-pista`}>{pista}</p>}
    </fieldset>
  )
}
