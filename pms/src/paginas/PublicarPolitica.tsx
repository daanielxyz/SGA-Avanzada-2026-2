import { useEffect, useId, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { consultarAlojamiento } from '../api/alojamiento'
import { ErrorApi } from '../api/cliente'
import { consultarPoliticaVigente, publicarPolitica, type BaseRetencion, type Politica } from '../api/politicas'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { useTitulo } from '../componentes/useTitulo'
import PoliticaContada from '../politica/PoliticaContada'
import {
  BASES, cuerpo, formDesde, hayErrores, nuevaClave, validar, vistaPrevia, type FormPolitica, type Retencion, type Unidad,
} from '../politica/formulario'
import { errorParaMostrar, type ErrorMostrado } from '../reserva/mensajes'
import { useAvisoAlSalir } from '../reserva/nueva/useAvisoAlSalir'
import SinPermiso from './SinPermiso'

type Carga = { vigente: Politica | null; minimoTramos: number }

/** Retener [valor] [% / $] de [base]: el mismo trío en cada tramo y en el no-show. */
function CamposRetencion({ retencion, error, rotulo, alCambiar }: {
  retencion: Retencion
  error?: string
  /** Para los lectores de pantalla: «tramo desde 48 horas». */
  rotulo: string
  alCambiar: (cambio: Partial<Retencion>) => void
}) {
  const id = useId()
  return (
    <>
      <span className="renglon-campos">
        <input className="campo-control renglon-numero" inputMode="numeric" value={retencion.valor}
          aria-label={`${retencion.unidad === 'porcentaje' ? 'Porcentaje' : 'Monto en pesos'} que se retiene, ${rotulo}`}
          aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined}
          onChange={(evento) => alCambiar({ valor: evento.target.value })} />
        <select className="campo-control renglon-unidad" value={retencion.unidad} aria-label={`Porcentaje o monto fijo, ${rotulo}`}
          onChange={(evento) => alCambiar({ unidad: evento.target.value as Unidad })}>
          <option value="porcentaje">%</option>
          <option value="monto">$</option>
        </select>
        <span className="renglon-texto">de</span>
        <select className="campo-control renglon-base" value={retencion.base} aria-label={`Sobre qué se calcula, ${rotulo}`}
          onChange={(evento) => alCambiar({ base: evento.target.value as BaseRetencion })}>
          {BASES.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
        </select>
      </span>
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
    </>
  )
}

/**
 * Publicar una versión nueva de la política (CU-35 · POL-01..04). Parte de la vigente; se revisa al escribir con las
 * reglas del dominio y la vista previa la cuenta como la verá recepción en «Ver política».
 */
export default function PublicarPolitica() {
  useTitulo('Publicar nueva versión')
  const navegar = useNavigate()
  const [intento, setIntento] = useState(0)
  const [carga, setCarga] = useState<Carga | null>(null)
  const [errorCarga, setErrorCarga] = useState<unknown>(null)
  const [form, setForm] = useState<FormPolitica | null>(null)
  const [inicial, setInicial] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<ErrorMostrado | null>(null)
  const [publicada, setPublicada] = useState(false)

  useEffect(() => {
    let vigente = true
    Promise.all([
      consultarPoliticaVigente().catch((causa: unknown) => {
        if (causa instanceof ErrorApi && causa.estado === 404) return null
        throw causa
      }),
      consultarAlojamiento(),
    ])
      .then(([politica, alojamiento]) => {
        if (!vigente) return
        const nuevo = formDesde(politica)
        setCarga({ vigente: politica, minimoTramos: alojamiento.parametros.minimoTramosCancelacion })
        setForm(nuevo)
        setInicial(JSON.stringify(cuerpo(nuevo)))
        setErrorCarga(null)
      })
      .catch((causa: unknown) => {
        if (vigente) setErrorCarga(causa)
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const errores = useMemo(() => (form && carga ? validar(form, carga.minimoTramos) : null), [form, carga])
  const sucio = form !== null && JSON.stringify(cuerpo(form)) !== inicial
  const salida = useAvisoAlSalir(sucio && !publicada)

  if (errorCarga instanceof ErrorApi && errorCarga.estado === 403) return <SinPermiso />

  const volver = <Link to="/politica" className="volver"><span aria-hidden="true">←</span> Volver a la política</Link>

  if (errorCarga) {
    return (
      <section className="politica-pagina">
        {volver}
        <h1>Publicar nueva versión</h1>
        <div className="tarjeta bloque-vacio" role="alert">
          <h2>No se pudo cargar la política vigente</h2>
          <p className="suave">Revisa la conexión e intenta de nuevo.</p>
          <button type="button" className="boton" onClick={() => setIntento(intento + 1)}>Reintentar</button>
        </div>
      </section>
    )
  }

  if (!form || !carga || !errores) {
    return (
      <section className="politica-pagina" aria-busy="true">
        {volver}
        <h1>Publicar nueva versión</h1>
        <p className="suave carga-texto" role="status">Cargando la política vigente…</p>
      </section>
    )
  }

  const conErrores = hayErrores(errores)
  const { vigente, minimoTramos } = carga

  function cambiarTramo(clave: number, cambio: Partial<FormPolitica['tramos'][number]>) {
    setForm((actual) => actual && { ...actual, tramos: actual.tramos.map((t) => (t.clave === clave ? { ...t, ...cambio } : t)) })
    setRechazo(null)
  }

  function agregarTramo() {
    setForm((actual) => actual && {
      ...actual,
      tramos: [...actual.tramos, { clave: nuevaClave(), horas: '', valor: '0', unidad: 'porcentaje', base: 'VALOR_TOTAL' }],
    })
  }

  function quitarTramo(clave: number) {
    setForm((actual) => actual && { ...actual, tramos: actual.tramos.filter((t) => t.clave !== clave) })
    setRechazo(null)
  }

  async function publicar(evento: FormEvent) {
    evento.preventDefault()
    if (!form || conErrores) return
    setEnviando(true)
    setRechazo(null)
    try {
      const nueva = await publicarPolitica(cuerpo(form))
      setPublicada(true)
      navegar('/politica', { state: { publicada: nueva.version } })
    } catch (causa) {
      setRechazo(errorParaMostrar(causa))
      setEnviando(false)
    }
  }

  const resumenPie = vigente
    ? `Se publica como versión nueva. La versión ${vigente.version} pasa a «Versiones anteriores».`
    : 'Será la primera versión de la política.'

  return (
    <section className="politica-pagina">
      {volver}
      <header className="politica-cabecera">
        <div>
          <h1>Publicar nueva versión</h1>
          <p className="suave">
            {vigente ? `Parte de la versión ${vigente.version}, la vigente. ` : ''}
            Cada tramo dice cuánto se retiene si el huésped cancela con al menos esas horas de antelación a la hora de
            entrada.
          </p>
        </div>
      </header>

      {rechazo && <Aviso tipo="error" asunto="No se publicó la versión nueva" mensaje={rechazo.mensaje} detalles={rechazo.detalles} />}

      <div className="publicar-politica">
        <form className="tarjeta bloque" onSubmit={publicar} noValidate aria-label="Versión nueva de la política">
          <fieldset className="bloque-cuerpo bloque-campos-politica" disabled={enviando}>
            <legend className="solo-lectores">Versión nueva de la política</legend>
            <section aria-labelledby="titulo-tramos">
              <h2 id="titulo-tramos">Si se cancela</h2>
              <p className="suave politica-pista">
                Debe haber un tramo desde 0 horas. Mínimo {minimoTramos} {minimoTramos === 1 ? 'tramo' : 'tramos'}; el
                mínimo se cambia en Alojamiento.
              </p>
              <ol className="renglones">
                {form.tramos.map((tramo, indice) => {
                  const propios = errores.tramos[tramo.clave] ?? {}
                  const rotulo = `tramo ${indice + 1}`
                  return (
                    <li key={tramo.clave} className={propios.horas || propios.valor ? 'renglon renglon-error' : 'renglon'}>
                      <div className="renglon-frase">
                        <span className="renglon-texto">Con al menos</span>
                        <input className="campo-control renglon-numero" inputMode="numeric" value={tramo.horas}
                          aria-label={`Horas de antelación, ${rotulo}`} aria-invalid={Boolean(propios.horas)}
                          aria-describedby={propios.horas ? `horas-error-${tramo.clave}` : undefined}
                          onChange={(evento) => cambiarTramo(tramo.clave, { horas: evento.target.value })} />
                        <span className="renglon-texto">horas de antelación, retener</span>
                        <button type="button" className="boton boton-secundario boton-icono renglon-quitar"
                          aria-label={`Quitar ${rotulo}`} onClick={() => quitarTramo(tramo.clave)}>
                          <span aria-hidden="true">✕</span>
                        </button>
                      </div>
                      {propios.horas && <p className="campo-mensaje" id={`horas-error-${tramo.clave}`}>{propios.horas}</p>}
                      <CamposRetencion retencion={tramo} error={propios.valor} rotulo={rotulo}
                        alCambiar={(cambio) => cambiarTramo(tramo.clave, cambio)} />
                    </li>
                  )
                })}
              </ol>
              {errores.lista.map((mensaje) => <p key={mensaje} className="campo-mensaje">{mensaje}</p>)}
              <button type="button" className="boton boton-secundario boton-chico agregar-tramo" onClick={agregarTramo}>
                <span aria-hidden="true">+</span> Agregar tramo
              </button>
            </section>

            <section aria-labelledby="titulo-no-show">
              <h2 id="titulo-no-show">Si no se presenta</h2>
              <p className="suave politica-pista">Lo que se retiene si el huésped no llega.</p>
              <div className={errores.noShow ? 'renglon renglon-solo renglon-error' : 'renglon renglon-solo'}>
                <span className="renglon-texto">Si el huésped no llega, retener</span>
                <CamposRetencion retencion={form.noShow} error={errores.noShow} rotulo="si el huésped no llega"
                  alCambiar={(cambio) => setForm((actual) => actual && { ...actual, noShow: { ...actual.noShow, ...cambio } })} />
              </div>
            </section>

            <Aviso tipo="info" asunto="Antes de publicar"
              mensaje="Rige para las reservas nuevas. Las reservas ya hechas conservan su versión." />
          </fieldset>
          <footer className="bloque-pie">
            <p className="bloque-estado">{conErrores ? 'Corrige lo marcado para poder publicar.' : resumenPie}</p>
            <button type="submit" className="boton" disabled={enviando || conErrores}>
              {enviando ? 'Publicando…' : 'Publicar nueva versión'}
            </button>
          </footer>
        </form>

        <aside className="tarjeta vista-previa vista-previa-politica" aria-labelledby="titulo-vista-previa">
          <h2 id="titulo-vista-previa">Vista previa</h2>
          <p className="suave politica-pista">Así la verá recepción en «Ver política».</p>
          {conErrores
            ? <p className="suave">La vista previa vuelve cuando se corrija lo marcado.</p>
            : <PoliticaContada politica={vistaPrevia(form)} />}
        </aside>
      </div>

      {salida.destino && (
        <Dialogo titulo="¿Salir sin publicar?" alCerrar={salida.quedarse}>
          <div className="dialogo-contenido">
            <div className="dialogo-cuerpo">
              <p className="dialogo-texto">Hay cambios sin publicar en la política. Si sales ahora, se pierden.</p>
            </div>
            <div className="dialogo-pie">
              <button type="button" className="boton boton-secundario" data-foco-inicial onClick={salida.quedarse}>Seguir editando</button>
              <button type="button" className="boton boton-peligro" onClick={salida.salir}>Salir sin publicar</button>
            </div>
          </div>
        </Dialogo>
      )}
    </section>
  )
}
