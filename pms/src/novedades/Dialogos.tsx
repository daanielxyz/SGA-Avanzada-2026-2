// Los diálogos de Novedades: reportar, avanzar un paso y ver el historial (pms/DISENO.md, «Sprint 5: novedades»).
import { useEffect, useId, useState, type FormEvent } from 'react'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { avanzarNovedad, reportarNovedad, type GravedadNovedad, type Novedad } from '../api/novedades'
import DialogoFormulario, { CampoConContador, type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import { errorParaMostrar } from '../reserva/mensajes'
import Campo from '../reserva/nueva/Campo'
import { accionSiguiente, GRAVEDADES, MAXIMO_DESCRIPCION, textoPaso } from './logica'
import { EtiquetaEstadoNovedad, EtiquetaGravedad } from './Piezas'

/** Lo que encontraron: apartamento, descripción y gravedad. No bloquea el apartamento (NOV-03). */
export function DialogoReportarNovedad({ alCerrar, alReportar }: {
  alCerrar: () => void
  alReportar: (novedad: Novedad) => void
}) {
  const id = useId()
  const [apartamentos, setApartamentos] = useState<string[] | null>(null)
  const [errorApartamentos, setErrorApartamentos] = useState(false)
  const [apartamento, setApartamento] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [gravedad, setGravedad] = useState<GravedadNovedad | ''>('')
  const [errores, setErrores] = useState<{ apartamento?: string; descripcion?: string; gravedad?: string }>({})
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)

  useEffect(() => {
    let vigente = true
    listarCodigosDeApartamentos()
      .then((codigos) => vigente && setApartamentos(codigos))
      .catch(() => vigente && setErrorApartamentos(true))
    return () => {
      vigente = false
    }
  }, [])

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const formulario = evento.currentTarget
    const encontrados = {
      apartamento: apartamento ? undefined : 'Elige el apartamento.',
      descripcion: descripcion.trim() ? undefined : 'Describe lo que encontraste.',
      gravedad: gravedad ? undefined : 'Elige la gravedad.',
    }
    setErrores(encontrados)
    setRechazo(null)
    if (Object.values(encontrados).some(Boolean)) {
      enfocarPrimerError(formulario)
      return
    }
    setEnviando(true)
    try {
      alReportar(await reportarNovedad(apartamento, descripcion.trim(), gravedad as GravedadNovedad))
    } catch (causa) {
      setEnviando(false)
      setRechazo({ asunto: 'No se reportó la novedad', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario titulo="Reportar novedad" enviando={enviando} rechazo={rechazo} textoBoton="Reportar"
      textoEnviando="Reportando…" alCerrar={alCerrar} alEnviar={enviar}>
      <div className="formulario-novedad">
        <Aviso tipo="info" mensaje="Reportarla no bloquea el apartamento. Si no se puede usar, avisa a Administración." />
        {errorApartamentos ? (
          <Aviso tipo="error" asunto="No se pudieron cargar los apartamentos" mensaje="Cierra y vuelve a abrir el diálogo." />
        ) : (
          <Campo etiqueta="Apartamento" value={apartamento} error={errores.apartamento} disabled={enviando || !apartamentos}
            data-foco-inicial
            opciones={[['', apartamentos ? 'Elige' : 'Cargando…'], ...(apartamentos ?? []).map((c) => [c, c] as const)]}
            onChange={(e) => {
              setApartamento(e.target.value)
              setErrores({ ...errores, apartamento: undefined })
            }} />
        )}
        <CampoConContador id={`${id}-descripcion`} rotulo="¿Qué encontraste?" valor={descripcion}
          maximo={MAXIMO_DESCRIPCION} varias error={errores.descripcion} deshabilitado={enviando}
          pista="Por ejemplo: el grifo del lavamanos gotea aunque esté cerrado."
          alCambiar={(valor) => {
            setDescripcion(valor)
            setErrores({ ...errores, descripcion: undefined })
          }} />
        <fieldset className="campo" disabled={enviando} aria-describedby={errores.gravedad ? `${id}-gravedad` : undefined}>
          <legend className="campo-rotulo">Gravedad</legend>
          <div className="roles">
            {GRAVEDADES.map((opcion) => (
              <label key={opcion.gravedad} className={gravedad === opcion.gravedad ? 'rol-opcion elegido' : 'rol-opcion'}>
                <input type="radio" name={`${id}-gravedad`} value={opcion.gravedad} checked={gravedad === opcion.gravedad}
                  aria-invalid={Boolean(errores.gravedad)} onChange={() => {
                    setGravedad(opcion.gravedad)
                    setErrores({ ...errores, gravedad: undefined })
                  }} />
                <span>
                  <EtiquetaGravedad gravedad={opcion.gravedad} />
                  <span className="campo-pista">{opcion.explica}</span>
                </span>
              </label>
            ))}
          </div>
          {errores.gravedad && <p className="campo-mensaje" id={`${id}-gravedad`}>{errores.gravedad}</p>}
        </fieldset>
      </div>
    </DialogoFormulario>
  )
}

/** Lo esencial de la novedad, arriba de cada diálogo. */
function Resumen({ novedad, completa = false }: { novedad: Novedad; completa?: boolean }) {
  return (
    <div className="novedad-resumen">
      <p className="novedad-apartamento codigo">{novedad.apartamento}</p>
      <p className="novedad-etiquetas">
        <EtiquetaGravedad gravedad={novedad.gravedad} />
        <EtiquetaEstadoNovedad estado={novedad.estado} />
      </p>
      <p className={completa ? 'novedad-descripcion-completa' : 'novedad-descripcion texto-largo'}>{novedad.descripcion}</p>
    </div>
  )
}

/**
 * Confirmación antes de avanzar. Si otra persona ya la movió (409), el diálogo se cierra y la lista se recarga con
 * el aviso.
 */
export function DialogoAvanzarNovedad({ novedad, alCerrar, alAvanzar, alYaAvanzo }: {
  novedad: Novedad
  alCerrar: () => void
  alAvanzar: (novedad: Novedad) => void
  alYaAvanzo: () => void
}) {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  const accion = accionSiguiente(novedad.estado)
  if (!accion) return null
  const cerrar = novedad.estado === 'EN_REVISION'

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setEnviando(true)
    setRechazo(null)
    try {
      alAvanzar(await avanzarNovedad(novedad.id))
    } catch (causa) {
      setEnviando(false)
      if (causa instanceof ErrorApi && causa.estado === 409) alYaAvanzo()
      else setRechazo({ asunto: 'No se avanzó la novedad', ...errorParaMostrar(causa) })
    }
  }

  return (
    <DialogoFormulario
      titulo={cerrar ? `¿Cerrar la novedad de ${novedad.apartamento}?` : `¿Pasar a revisión la novedad de ${novedad.apartamento}?`}
      enviando={enviando} rechazo={rechazo} textoBoton={accion.confirmar} textoEnviando={accion.enviando}
      alCerrar={alCerrar} alEnviar={enviar}>
      <Resumen novedad={novedad} />
      {cerrar && <Aviso tipo="advertencia" mensaje="Una novedad cerrada no se puede reabrir." />}
    </DialogoFormulario>
  )
}

/** El historial completo: cada paso con quién y cuándo, y desde aquí también la acción siguiente. */
export function DialogoHistorialNovedad({ novedad, alCerrar, alAvanzar }: {
  novedad: Novedad
  alCerrar: () => void
  alAvanzar: (novedad: Novedad) => void
}) {
  const accion = accionSiguiente(novedad.estado)
  return (
    <Dialogo titulo={`Novedad de ${novedad.apartamento}`} alCerrar={alCerrar}>
      <div className="dialogo-cuerpo">
        <Resumen novedad={novedad} completa />
        <h3 className="novedad-pasos-titulo">Pasos</h3>
        <ol className="novedad-pasos">
          {novedad.historial.map((paso, i) => {
            const { estado, quien, cuando } = textoPaso(paso)
            return (
              <li key={`${paso.estado}-${i}`}>
                <strong>{estado}</strong>
                <span>
                  <span className={paso.autorNombre ? undefined : 'suave'}>{quien}</span>
                  <span className="suave"> · {cuando}</span>
                </span>
              </li>
            )
          })}
        </ol>
      </div>
      <div className="dialogo-pie">
        <button type="button" className="boton boton-secundario" onClick={alCerrar}>Cerrar</button>
        {accion && (
          <button type="button" className="boton" data-foco-inicial onClick={() => alAvanzar(novedad)}>
            {accion.confirmar}
          </button>
        )}
      </div>
    </Dialogo>
  )
}
