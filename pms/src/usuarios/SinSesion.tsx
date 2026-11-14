// Lo común de «Activar mi cuenta» y «Olvidé mi contraseña»: la plantilla de «Iniciar sesión» y los campos del código.
import { useEffect, useId, useRef, type FormEventHandler, type ReactNode } from 'react'
import { Link } from 'react-router'
import Logo from '../componentes/Logo'
import { codigoAlEscribir, MAXIMO_CORREO } from './logica'

/** La misma plantilla de «Iniciar sesión»: el arte a la izquierda en el computador y el formulario centrado. */
export function PaginaSinSesion({ titulo, texto, paso, ocupado, alEnviar, children }: {
  titulo: string
  texto?: ReactNode
  /** «Paso 1 de 2». */
  paso?: string
  ocupado: boolean
  alEnviar: FormEventHandler<HTMLFormElement>
  children: ReactNode
}) {
  const encabezado = useRef<HTMLHeadingElement>(null)
  const pasoAnterior = useRef(paso)
  // Al pasar al paso 2 el formulario es otro: el foco va a su título para que el lector lo anuncie
  useEffect(() => {
    if (pasoAnterior.current !== paso) encabezado.current?.focus()
    pasoAnterior.current = paso
  }, [paso])
  return (
    <main className="entrada">
      <div className="entrada-arte">
        <Logo decorativo />
        <div className="entrada-lema">
          <p className="entrada-lema-titulo">Cada detalle bajo control.</p>
          <p className="entrada-lema-texto">La operación en su punto más alto.</p>
        </div>
      </div>
      <div className="entrada-formulario">
        <form className="formulario" onSubmit={alEnviar} noValidate aria-busy={ocupado}>
          <Logo apilado />
          <div className="entrada-titulo">
            {paso && <p className="paso-de">{paso}</p>}
            <h1 ref={encabezado} tabIndex={-1}>{titulo}</h1>
            {texto && <p className="suave">{texto}</p>}
          </div>
          {children}
          <p className="entrada-enlaces">
            <Link to="/ingresar" className="enlace">Volver a iniciar sesión</Link>
          </p>
        </form>
      </div>
    </main>
  )
}

export function CampoCorreoEntrada({ valor, error, deshabilitado, alCambiar }: {
  valor: string
  error?: string
  deshabilitado: boolean
  alCambiar: (valor: string) => void
}) {
  const id = useId()
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>Correo electrónico</label>
      <input id={id} className="campo-control campo-control-grande" type="email" inputMode="email" autoComplete="username"
        spellCheck={false} maxLength={MAXIMO_CORREO} placeholder="tu@correo.com" value={valor} disabled={deshabilitado}
        aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined}
        onChange={(e) => alCambiar(e.target.value)} />
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
    </div>
  )
}

/** El código de 6 dígitos del correo: solo números, y el teléfono ofrece el que acaba de llegar. */
export function CampoCodigo({ valor, error, pista, deshabilitado, alCambiar }: {
  valor: string
  error?: string
  pista: string
  deshabilitado: boolean
  alCambiar: (valor: string) => void
}) {
  const id = useId()
  return (
    <div className="campo">
      <label className="campo-rotulo" htmlFor={id}>Código de 6 dígitos</label>
      <input id={id} className="campo-control campo-control-grande campo-codigo numero" inputMode="numeric"
        autoComplete="one-time-code" maxLength={6} value={valor} disabled={deshabilitado} aria-invalid={Boolean(error)}
        aria-describedby={`${error ? `${id}-error ` : ''}${id}-pista`}
        onChange={(e) => alCambiar(codigoAlEscribir(e.target.value))} />
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
      <p className="campo-pista" id={`${id}-pista`}>{pista}</p>
    </div>
  )
}
