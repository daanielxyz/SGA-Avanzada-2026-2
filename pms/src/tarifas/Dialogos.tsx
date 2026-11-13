// Los diálogos de temporadas y tarifas, uno por acción (pms/DISENO.md, «Sprint 4: temporadas y tarifas»).
import { useEffect, useId, useState, type FormEvent, type ReactNode } from 'react'
import { listarTodosLosApartamentos } from '../api/apartamentos'
import type { Pagina } from '../api/reservas'
import {
  agregarTemporada,
  cambiarFechasTemporada,
  definirTarifa,
  desactivarTemporada,
  historialTarifas,
  type Tarifa,
  type Temporada,
} from '../api/tarifas'
import DialogoFormulario, { CampoConContador, type Rechazo } from '../apartamentos/DialogoFormulario'
import { enfocarPrimerError } from '../apartamentos/formulario'
import Aviso from '../componentes/Aviso'
import Dialogo from '../componentes/Dialogo'
import Paginador from '../componentes/Paginador'
import { dinero, fechaCorta } from '../formato'
import Campo from '../reserva/nueva/Campo'
import { errorParaMostrar } from '../reserva/mensajes'
import {
  conNombres,
  cuerpoTemporada,
  errorDeFechas,
  errorDeValor,
  hayErrores,
  leerPesos,
  MAXIMO_NOMBRE_TEMPORADA,
  pesosAlEscribir,
  textoEstancia,
  textoFechas,
  validarTemporada,
  type ApartamentoDeTarifa,
  type ErroresTemporada,
  type FormTemporada,
} from './logica'

const LO_YA_RESERVADO = 'Solo cambia el valor de las reservas nuevas. Las reservas ya hechas conservan el valor con que se reservaron.'
const PISTA_FECHAS = 'Los dos días cuentan. Si otra temporada termina el 15, esta puede empezar el 16.'

/** El rechazo de la API como se muestra, con los nombres de las temporadas en vez de sus ids. */
function rechazoDe(causa: unknown, asunto: string, temporadas: Temporada[], nombreNueva?: string): Rechazo {
  const { mensaje, detalles } = errorParaMostrar(causa)
  return { asunto, mensaje: conNombres(mensaje, temporadas, nombreNueva), detalles }
}

/** Envía, y si la API lo rechaza, deja el rechazo en el diálogo. */
function useEnvio(temporadas: Temporada[]) {
  const [enviando, setEnviando] = useState(false)
  const [rechazo, setRechazo] = useState<Rechazo | null>(null)
  async function intentar(asunto: string, accion: () => Promise<void>, nombreNueva?: string) {
    setEnviando(true)
    setRechazo(null)
    try {
      await accion()
    } catch (causa) {
      setRechazo(rechazoDe(causa, asunto, temporadas, nombreNueva))
    } finally {
      setEnviando(false)
    }
  }
  return { enviando, rechazo, intentar }
}

/** Un valor en pesos: el «$» delante y los miles separados mientras se escribe. */
function CampoPesos({ id, rotulo, valor, error, pista, focoInicial, deshabilitado, alCambiar }: {
  id: string
  rotulo: ReactNode
  valor: string
  error?: string
  pista?: ReactNode
  focoInicial?: boolean
  deshabilitado?: boolean
  alCambiar: (valor: string) => void
}) {
  const descritoPor = [error && `${id}-error`, pista && `${id}-pista`].filter(Boolean).join(' ') || undefined
  return (
    <div className="campo campo-pesos">
      <label className="campo-rotulo" htmlFor={id}>{rotulo}</label>
      <div className="campo-unidad">
        <span className="campo-unidad-antes" aria-hidden="true">$</span>
        <input
          id={id}
          className="campo-control numero"
          inputMode="numeric"
          autoComplete="off"
          maxLength={15}
          value={valor}
          disabled={deshabilitado}
          data-foco-inicial={focoInicial || undefined}
          aria-invalid={Boolean(error)}
          aria-describedby={descritoPor}
          onChange={(e) => alCambiar(pesosAlEscribir(e.target.value))}
        />
      </div>
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
      {pista && <p className="campo-pista" id={`${id}-pista`}>{pista}</p>}
    </div>
  )
}

/** Desde y hasta, con un solo mensaje para las dos: el error de una casi siempre es de la pareja. */
function CamposFechas({ inicio, fin, error, deshabilitado, alCambiar }: {
  inicio: string
  fin: string
  error?: string
  deshabilitado?: boolean
  alCambiar: (cambio: { inicio?: string; fin?: string }) => void
}) {
  const id = useId()
  return (
    <fieldset className="campo fechas-temporada" disabled={deshabilitado} aria-describedby={`${id}-mensaje`}>
      <legend className="solo-lectores">Fechas de la temporada</legend>
      <div className="formulario-apartamento-pareja">
        <Campo etiqueta="Desde" type="date" value={inicio} invalido={Boolean(error)}
          onChange={(e) => alCambiar({ inicio: e.target.value })} />
        <Campo etiqueta="Hasta" type="date" value={fin} min={inicio || undefined} invalido={Boolean(error)}
          onChange={(e) => alCambiar({ fin: e.target.value })} />
      </div>
      <p id={`${id}-mensaje`} className={error ? 'campo-mensaje' : 'campo-pista'} role={error ? 'alert' : undefined}>
        {error ?? PISTA_FECHAS}
      </p>
    </fieldset>
  )
}

const FORM_TEMPORADA: FormTemporada = { nombre: '', inicio: '', fin: '', estancia: '', tarifas: {} }

/**
 * Nueva temporada con la tarifa de cada apartamento (CU-33). La API exige la de los que están a la venta (TAR-03);
 * la de los retirados es opcional y, si queda vacía, el apartamento queda «Sin tarifa».
 */
export function DialogoTemporada({ temporadas, alCerrar, alCrear }: {
  temporadas: Temporada[]
  alCerrar: () => void
  alCrear: (temporada: Temporada) => void
}) {
  const id = useId()
  const [apartamentos, setApartamentos] = useState<ApartamentoDeTarifa[] | null>(null)
  const [errorCarga, setErrorCarga] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)
  const [form, setForm] = useState<FormTemporada>(FORM_TEMPORADA)
  const [errores, setErrores] = useState<ErroresTemporada>({ tarifas: {} })
  const { enviando, rechazo, intentar } = useEnvio(temporadas)

  useEffect(() => {
    let vigente = true
    listarTodosLosApartamentos()
      .then((todos) => vigente && setApartamentos(todos.map(({ codigo, nombre, activo }) => ({ codigo, nombre, activo }))))
      .catch((causa: unknown) => vigente && setErrorCarga(causa))
    return () => {
      vigente = false
    }
  }, [intento])

  function cambiar(cambio: Partial<FormTemporada>, limpiar: keyof Omit<ErroresTemporada, 'tarifas'>) {
    setForm((actual) => ({ ...actual, ...cambio }))
    setErrores((actuales) => ({ ...actuales, [limpiar]: undefined }))
  }

  function cambiarTarifa(codigo: string, valor: string) {
    setForm((actual) => ({ ...actual, tarifas: { ...actual.tarifas, [codigo]: valor } }))
    setErrores((actuales) => {
      const resto = Object.fromEntries(Object.entries(actuales.tarifas).filter(([clave]) => clave !== codigo))
      return { ...actuales, tarifas: resto }
    })
  }

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (!apartamentos) return
    const formulario = evento.currentTarget
    const encontrados = validarTemporada(form, temporadas, apartamentos)
    setErrores(encontrados)
    if (hayErrores(encontrados)) {
      enfocarPrimerError(formulario)
      return
    }
    void intentar('No se creó la temporada', async () => {
      const { temporada } = await agregarTemporada(cuerpoTemporada(form))
      alCrear(temporada)
    }, form.nombre)
  }

  return (
    <DialogoFormulario titulo="Nueva temporada" enviando={enviando} rechazo={rechazo}
      bloqueado={!apartamentos} textoBoton="Crear temporada" textoEnviando="Guardando…" alCerrar={alCerrar}
      alEnviar={enviar}>
      <fieldset className="formulario-apartamento" disabled={enviando}>
        <CampoConContador id={`${id}-nombre`} rotulo="Nombre" valor={form.nombre} maximo={MAXIMO_NOMBRE_TEMPORADA}
          error={errores.nombre} focoInicial alCambiar={(nombre) => cambiar({ nombre }, 'nombre')} />
        <CamposFechas inicio={form.inicio} fin={form.fin} error={errores.fechas}
          alCambiar={(fechas) => cambiar(fechas, 'fechas')} />
        <div className="campo">
          <label className="campo-rotulo" htmlFor={`${id}-estancia`}>Estancia mínima</label>
          <div className="campo-unidad campo-unidad-corta">
            <input id={`${id}-estancia`} className="campo-control numero" inputMode="numeric" autoComplete="off"
              maxLength={3} value={form.estancia} aria-invalid={Boolean(errores.estancia)}
              aria-describedby={`${id}-estancia-mensaje`}
              onChange={(e) => cambiar({ estancia: e.target.value.replace(/\D/g, '') }, 'estancia')} />
            <span className="campo-unidad-despues" aria-hidden="true">noches</span>
          </div>
          <p id={`${id}-estancia-mensaje`} className={errores.estancia ? 'campo-mensaje' : 'campo-pista'}>
            {errores.estancia ?? 'Escribe 0 si no hay mínimo.'}
          </p>
        </div>

        <fieldset className="campo tarifas-nuevas">
          <legend className="tarifas-nuevas-titulo">Tarifa de cada apartamento</legend>
          <p className="campo-pista">
            Valor por persona y noche en esta temporada. Los que están a la venta la necesitan; un retirado que quede
            vacío queda «Sin tarifa» y no se puede poner a la venta.
          </p>
          {!apartamentos && !errorCarga && <p className="suave" role="status">Cargando los apartamentos…</p>}
          {errorCarga !== null && !apartamentos && (
            <Aviso tipo="error" asunto="No pudimos cargar los apartamentos" mensaje={errorParaMostrar(errorCarga).mensaje}
              accion={<button type="button" className="boton boton-secundario boton-chico"
                onClick={() => { setErrorCarga(null); setIntento(intento + 1) }}>Reintentar</button>} />
          )}
          {apartamentos && apartamentos.length === 0 && (
            <p className="suave">Todavía no hay apartamentos: la temporada se crea sin tarifas.</p>
          )}
          {apartamentos && apartamentos.length > 0 && (
            <ul className="tarifas-nuevas-lista">
              {apartamentos.map((apartamento) => (
                <li key={apartamento.codigo}>
                  <CampoPesos
                    id={`${id}-${apartamento.codigo}`}
                    rotulo={<>
                      <span className="codigo">{apartamento.codigo}</span> {apartamento.nombre}{' '}
                      <span className="campo-rotulo-nota">{apartamento.activo ? '(a la venta)' : '(opcional)'}</span>
                    </>}
                    valor={form.tarifas[apartamento.codigo] ?? ''}
                    error={errores.tarifas[apartamento.codigo]}
                    alCambiar={(valor) => cambiarTarifa(apartamento.codigo, valor)}
                  />
                </li>
              ))}
            </ul>
          )}
        </fieldset>
        <Aviso tipo="info" mensaje={LO_YA_RESERVADO} />
      </fieldset>
    </DialogoFormulario>
  )
}

/** Cambiar las fechas de una temporada (TEM-05): las reservas ya hechas conservan su valor. */
export function DialogoFechas({ temporada, temporadas, alCerrar, alHacer }: {
  temporada: Temporada
  temporadas: Temporada[]
  alCerrar: () => void
  alHacer: (calendario: Temporada[]) => void
}) {
  const [fechas, setFechas] = useState({ inicio: temporada.fechaInicio ?? '', fin: temporada.fechaFin ?? '' })
  const [error, setError] = useState<string | null>(null)
  const { enviando, rechazo, intentar } = useEnvio(temporadas)

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const encontrado = errorDeFechas(temporada.nombre, fechas.inicio, fechas.fin, temporadas, temporada.id)
    setError(encontrado)
    if (encontrado) {
      enfocarPrimerError(evento.currentTarget)
      return
    }
    void intentar('No se cambiaron las fechas', async () => {
      alHacer(await cambiarFechasTemporada(temporada.id, fechas.inicio, fechas.fin))
    })
  }

  return (
    <DialogoFormulario titulo="Cambiar fechas" subtitulo={temporada.nombre} enviando={enviando} rechazo={rechazo}
      textoBoton="Guardar fechas" textoEnviando="Guardando…" alCerrar={alCerrar} alEnviar={enviar}>
      <CamposFechas inicio={fechas.inicio} fin={fechas.fin} error={error ?? undefined} deshabilitado={enviando}
        alCambiar={(cambio) => { setFechas((actual) => ({ ...actual, ...cambio })); setError(null) }} />
      <Aviso tipo="info" mensaje="Las reservas ya hechas conservan el valor con que se reservaron." />
    </DialogoFormulario>
  )
}

/** Desactivar una temporada (TEM-06): sus noches pasan a la base y no se vuelve a activar. */
export function DialogoDesactivar({ temporada, temporadas, alCerrar, alHacer }: {
  temporada: Temporada
  temporadas: Temporada[]
  alCerrar: () => void
  alHacer: (calendario: Temporada[]) => void
}) {
  const { enviando, rechazo, intentar } = useEnvio(temporadas)
  return (
    <DialogoFormulario titulo={`¿Desactivar ${temporada.nombre}?`} enviando={enviando} rechazo={rechazo} peligro
      textoBoton="Desactivar temporada" textoEnviando="Desactivando…" alCerrar={alCerrar}
      alEnviar={(e) => {
        e.preventDefault()
        void intentar('No se desactivó la temporada', async () => {
          alHacer(await desactivarTemporada(temporada.id))
        })
      }}>
      <dl className="resumen-dialogo">
        <dt>Fechas</dt>
        <dd>{textoFechas(temporada)}</dd>
        <dt>Estancia mínima</dt>
        <dd>{textoEstancia(temporada.estanciaMinimaNoches)}</dd>
      </dl>
      <p className="dialogo-texto">
        {temporada.nombre} deja de aplicarse a las reservas nuevas: esas noches se cobran con la temporada base. Las
        reservas ya hechas no cambian.
      </p>
      <Aviso tipo="advertencia" mensaje="No se puede volver a activar." />
    </DialogoFormulario>
  )
}

/** Cambiar o poner la tarifa de un apartamento en una temporada (CU-34): una versión nueva que rige desde hoy. */
export function DialogoTarifa({ apartamento, temporada, vigente, temporadas, alCerrar, alHacer }: {
  apartamento: { codigo: string; nombre: string }
  temporada: Temporada
  vigente?: Tarifa
  temporadas: Temporada[]
  alCerrar: () => void
  alHacer: (tarifa: Tarifa) => void
}) {
  const id = useId()
  const [valor, setValor] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { enviando, rechazo, intentar } = useEnvio(temporadas)

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const encontrado = errorDeValor(valor)
    setError(encontrado)
    if (encontrado) {
      enfocarPrimerError(evento.currentTarget)
      return
    }
    void intentar('No se guardó la tarifa', async () => {
      alHacer(await definirTarifa(apartamento.codigo, temporada.id, leerPesos(valor)))
    })
  }

  return (
    <DialogoFormulario
      titulo={`${apartamento.codigo} · ${temporada.nombre}`}
      subtitulo={apartamento.nombre}
      enviando={enviando}
      rechazo={rechazo}
      textoBoton="Guardar tarifa"
      textoEnviando="Guardando…"
      alCerrar={alCerrar}
      alEnviar={enviar}
    >
      <CampoPesos id={`${id}-valor`} rotulo="Valor por persona y noche" valor={valor} error={error ?? undefined}
        focoInicial deshabilitado={enviando}
        pista={vigente ? `Hoy: ${dinero(vigente.valorPorOcupante)}, desde el ${fechaCorta(vigente.vigenteDesde)}.` : 'Hoy: sin tarifa.'}
        alCambiar={(nuevo) => { setValor(nuevo); setError(null) }} />
      <Aviso tipo="info" mensaje="Rige para las reservas nuevas desde hoy. Las reservas ya hechas no cambian." />
    </DialogoFormulario>
  )
}

type ResultadoHistorial = { clave: string; pagina: Pagina<Tarifa> } | { clave: string; error: unknown }

/** Todas las versiones de las tarifas de un apartamento, de la más reciente a la más antigua (TAR-05). */
export function DialogoHistorial({ apartamento, temporadas, vigentes, alCerrar }: {
  apartamento: { codigo: string; nombre: string }
  temporadas: Temporada[]
  /** Los ids de las tarifas vigentes hoy, para marcarlas. */
  vigentes: Set<string>
  alCerrar: () => void
}) {
  const [numero, setNumero] = useState(0)
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<ResultadoHistorial | null>(null)
  const clave = `${numero}#${intento}`

  useEffect(() => {
    let vigente = true
    historialTarifas(apartamento.codigo, numero)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [apartamento.codigo, numero, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const nombreDe = (id: string) => temporadas.find((t) => t.id === id)?.nombre ?? 'Temporada sin nombre'

  return (
    <Dialogo titulo={`Historial de tarifas · ${apartamento.codigo}`} subtitulo={apartamento.nombre} ancho
      alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo historial-tarifas">
          <p className="campo-pista">Valor por persona y noche, de la versión más reciente a la más antigua.</p>
          {!actual && <p className="suave" role="status">Cargando el historial…</p>}
          {actual && 'error' in actual && (
            <Aviso tipo="error" asunto="No pudimos cargar el historial" mensaje={errorParaMostrar(actual.error).mensaje}
              accion={<button type="button" className="boton boton-secundario boton-chico"
                onClick={() => setIntento(intento + 1)}>Reintentar</button>} />
          )}
          {actual && 'pagina' in actual && actual.pagina.contenido.length === 0 && (
            <p className="suave">Este apartamento todavía no tiene tarifas.</p>
          )}
          {actual && 'pagina' in actual && actual.pagina.contenido.length > 0 && (
            <>
              <div className="tarjeta desplazable">
                <table className="tabla tabla-historial">
                  <caption className="solo-lectores">Versiones de las tarifas de {apartamento.codigo}</caption>
                  <thead>
                    <tr>
                      <th scope="col">Temporada</th>
                      <th scope="col" className="derecha">Valor</th>
                      <th scope="col" className="derecha">Versión</th>
                      <th scope="col">Rigió desde</th>
                    </tr>
                  </thead>
                  <tbody>
                    {actual.pagina.contenido.map((tarifa) => (
                      <tr key={tarifa.id}>
                        <td>
                          <span className="texto-largo">{nombreDe(tarifa.temporada)}</span>
                          {vigentes.has(tarifa.id) && <span className="marca-vigente"><Visto /> Vigente</span>}
                        </td>
                        <td className="derecha numero">{dinero(tarifa.valorPorOcupante)}</td>
                        <td className="derecha numero">{tarifa.version}</td>
                        <td>{fechaCorta(tarifa.vigenteDesde)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {actual.pagina.totalPaginas > 1 && (
                <Paginador numero={actual.pagina.numero} totalPaginas={actual.pagina.totalPaginas}
                  totalElementos={actual.pagina.totalElementos} unidad={['versión', 'versiones']} alCambiar={setNumero} />
              )}
            </>
          )}
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" onClick={alCerrar}>Cerrar</button>
        </div>
      </div>
    </Dialogo>
  )
}

export function Visto() {
  return (
    <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
      <path d="M2 6.5 4.8 9.5 10 3" />
    </svg>
  )
}
