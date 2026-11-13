import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { consultarOcupacion, type OcupacionApartamento, type ReservaOcupada } from '../api/ocupacion'
import { NOMBRE_DEL_CANAL, NOMBRE_DEL_ESTADO, type Pagina } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import Dialogo from '../componentes/Dialogo'
import EstadoApartamento from '../componentes/EstadoApartamento'
import MotivoFueraDeServicio from '../componentes/MotivoFueraDeServicio'
import { FormaEstado } from '../componentes/EtiquetaEstado'
import { claseDeEstado } from '../componentes/estadosReserva'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, FECHA_MAXIMA, fechaCorta, fechaHoy, fechaSinAnio, noches, sumarDias } from '../formato'
import { barrasDe, nochesDelRango, type Barra } from '../ocupacion/barras'
import { CUENTAS, cuentaDe, NOMBRE_DE_LA_CUENTA, textoCortoDeCuenta, textoDeCuenta } from '../ocupacion/cuenta'

const NOCHES_POR_DEFECTO = 14
/** Cuánto corren el rango «Anterior» y «Siguiente». */
const DIAS_AL_MOVER = 7

const DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']

const FORMA_BLOQUEO = <path d="M6 1.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM2.8 2.8l6.4 6.4" />

const esFecha = (valor: string | null): valor is string => Boolean(valor && /^\d{4}-\d{2}-\d{2}$/.test(valor))

function diaDeLaSemana(fecha: string): string {
  return DIAS[new Date(`${fecha}T00:00:00Z`).getUTCDay()]
}

type Resultado = { clave: string; pagina: Pagina<OcupacionApartamento> } | { clave: string; error: unknown }

/**
 * Tablero de ocupación (DEC-86). El rango vive en la dirección, para volver desde el detalle al mismo sitio. Se elige la
 * última noche que se quiere ver; a la API se le pide un día más, porque su `hasta` es exclusiva (EST-01). Sin rango
 * elegido, empieza un día antes de la estancia en curso que empezó primero, para verlas enteras (#23). Tocar una
 * reserva abre una ventana con lo básico y el enlace al detalle.
 */
export default function Ocupacion() {
  useTitulo('Ocupación')
  const [parametros, setParametros] = useSearchParams()
  const hoy = fechaHoy()
  const desdeElegido = esFecha(parametros.get('desde'))
  const desde = desdeElegido ? (parametros.get('desde') as string) : hoy
  const ultima = esFecha(parametros.get('ultima'))
    ? (parametros.get('ultima') as string)
    : sumarDias(desde, NOCHES_POR_DEFECTO - 1)
  const apartamento = parametros.get('apartamento') ?? ''
  const numero = Math.max(Number(parametros.get('pagina')) || 0, 0)
  const hasta = sumarDias(ultima, 1)
  const rangoInvalido = ultima < desde
  const totalNoches = noches(desde, hasta)

  const [codigos, setCodigos] = useState<string[]>([])
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [abierta, setAbierta] = useState<ReservaOcupada | null>(null)
  const clave = `${desde}|${hasta}|${apartamento}|${numero}|${intento}`

  useEffect(() => {
    let vigente = true
    // Sin la lista, el filtro queda en «Todos» y el tablero sigue sirviendo
    listarCodigosDeApartamentos()
      .then((lista) => {
        if (vigente) setCodigos(lista)
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  useEffect(() => {
    if (rangoInvalido) return
    let vigente = true
    consultarOcupacion({ desde, hasta, apartamento, pagina: numero })
      .then((pagina) => {
        if (!vigente) return
        const antes = desdeElegido ? null : primeraEntradaAntesDe(pagina.contenido, desde)
        if (antes) {
          // Sin rango elegido, se corre al comienzo de lo que está en curso; la última noche no cambia
          setParametros({ ...Object.fromEntries(parametros), desde: sumarDias(antes, -1), ultima }, { replace: true })
          return
        }
        setResultado({ clave, pagina })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
    // Los parámetros solo se leen al llegar la respuesta: la consulta depende de la clave
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apartamento, clave, desde, hasta, numero, rangoInvalido])

  function cambiar(cambios: { desde?: string; ultima?: string; apartamento?: string; pagina?: number }) {
    const nuevos = { desde, ultima, apartamento, pagina: 0, ...cambios }
    setParametros({
      desde: nuevos.desde,
      ultima: nuevos.ultima,
      ...(nuevos.apartamento ? { apartamento: nuevos.apartamento } : {}),
      ...(nuevos.pagina ? { pagina: String(nuevos.pagina) } : {}),
    })
  }

  // De a una semana: correr el rango entero dejaba de ver lo que se estaba mirando (ronda 2)
  const moverRango = (dias: number) => cambiar({ desde: sumarDias(desde, dias), ultima: sumarDias(ultima, dias) })
  // «Hoy» vuelve a la vista de entrada: hoy y las estancias en curso enteras
  const irAHoy = () => setParametros(apartamento ? { apartamento } : {})

  const actual = resultado?.clave === clave ? resultado : null

  return (
    <div className="ocupacion">
      <div>
        <h1>Ocupación</h1>
        {!rangoInvalido && (
          <p className="suave">
            {fechaCorta(desde)} → {fechaCorta(hasta)} · {cantidad(totalNoches, 'noche', 'noches')}
          </p>
        )}
      </div>

      <div className="tarjeta ocupacion-controles">
        <CampoFecha etiqueta="Desde" valor={desde} alCambiar={(valor) => cambiar({ desde: valor })} />
        <CampoFecha
          etiqueta="Última noche"
          valor={ultima}
          error={rangoInvalido ? 'La última noche no puede ser anterior a «Desde».' : undefined}
          alCambiar={(valor) => cambiar({ ultima: valor })}
        />
        <div className="ocupacion-apartamento">
          <label className="campo-rotulo" htmlFor="ocupacion-apartamento">
            Apartamento
          </label>
          <select
            id="ocupacion-apartamento"
            className="campo-control"
            value={apartamento}
            onChange={(evento) => cambiar({ apartamento: evento.target.value })}
          >
            <option value="">Todos</option>
            {/* Un filtro que llega en la dirección se conserva aunque la lista no haya cargado */}
            {[...new Set([...(apartamento ? [apartamento] : []), ...codigos])].map((codigo) => (
              <option key={codigo} value={codigo}>
                {codigo}
              </option>
            ))}
          </select>
        </div>
        <div className="ocupacion-mover">
          <button type="button" className="boton boton-secundario" disabled={rangoInvalido} onClick={() => moverRango(-DIAS_AL_MOVER)}>
            <span aria-hidden="true">←</span> Anterior
          </button>
          <button type="button" className="boton boton-secundario" onClick={irAHoy}>
            Hoy
          </button>
          <button type="button" className="boton boton-secundario" disabled={rangoInvalido} onClick={() => moverRango(DIAS_AL_MOVER)}>
            Siguiente <span aria-hidden="true">→</span>
          </button>
        </div>
      </div>

      <Leyenda />

      {rangoInvalido && <Aviso tipo="error" asunto="Rango inválido" mensaje="Corrige las fechas para ver la ocupación." />}
      {!rangoInvalido && !actual && <Cargando />}
      {!rangoInvalido && actual && 'error' in actual && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar la ocupación"
          mensaje={actual.error instanceof ErrorApi ? actual.error.message : 'Inténtalo de nuevo en un momento.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}
      {!rangoInvalido && actual && 'pagina' in actual && actual.pagina.contenido.length === 0 && (
        <BloqueVacio icono="apartamentos" titulo={apartamento ? 'No encontramos ese apartamento' : 'Aún no hay apartamentos'}>
          {apartamento
            ? 'Elige «Todos» para ver el resto de apartamentos.'
            : 'Cuando se registren apartamentos, aquí verás su ocupación por noche.'}
        </BloqueVacio>
      )}
      {!rangoInvalido && actual && 'pagina' in actual && actual.pagina.contenido.length > 0 && (
        <>
          <Cuadricula filas={actual.pagina.contenido} desde={desde} hasta={hasta} hoy={hoy} alAbrir={setAbierta} />
          <ListaCelular filas={actual.pagina.contenido} desde={desde} hasta={hasta} />
          <Paginador
            numero={actual.pagina.numero}
            totalPaginas={actual.pagina.totalPaginas}
            totalElementos={actual.pagina.totalElementos}
            unidad={['apartamento', 'apartamentos']}
            alCambiar={(pagina) => cambiar({ pagina })}
          />
        </>
      )}
      {abierta && <VentanaReserva reserva={abierta} alCerrar={() => setAbierta(null)} />}
    </div>
  )
}

/** El navegador avisa una fecha vacía mientras se escribe: solo se aplica una completa, sin borrar lo que se teclea. */
function CampoFecha({ etiqueta, valor, error, alCambiar }: {
  etiqueta: string
  valor: string
  error?: string
  alCambiar: (valor: string) => void
}) {
  const [borrador, setBorrador] = useState(valor)
  const [ultimoValor, setUltimoValor] = useState(valor)
  if (valor !== ultimoValor) {
    setUltimoValor(valor)
    setBorrador(valor)
  }
  const id = `ocupacion-${etiqueta.replace(/\W/g, '')}`
  return (
    <div>
      <label className="campo-rotulo" htmlFor={id}>
        {etiqueta}
      </label>
      <input
        id={id}
        type="date"
        max={FECHA_MAXIMA}
        className="campo-control"
        value={borrador}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(evento) => {
          setBorrador(evento.target.value)
          if (evento.target.value) alCambiar(evento.target.value)
        }}
      />
      {error && <p className="campo-mensaje" id={`${id}-error`}>{error}</p>}
    </div>
  )
}

function Forma({ children }: { children: ReactNode }) {
  return (
    <svg className="etiqueta-forma" viewBox="0 0 12 12" aria-hidden="true">
      {children}
    </svg>
  )
}

/** Qué dice cada barra: el color es la cuenta y la forma, el estado (BL-09). En el celular va plegada. */
function Leyenda() {
  const contenido = (
    <div className="ocupacion-leyenda">
      <div>
        <p className="ocupacion-leyenda-titulo">Color de la barra: la cuenta</p>
        <ul>
          {CUENTAS.map((cuenta) => (
            <li key={cuenta}>
              <span className={`ocupacion-muestra cuenta-${cuenta}`} />
              {NOMBRE_DE_LA_CUENTA[cuenta]}
              {cuenta === 'al-dia' && <span className="suave"> (incluye a favor)</span>}
              {cuenta === 'no-show' && <span className="suave"> (apagada, último carril)</span>}
            </li>
          ))}
        </ul>
      </div>
      <div>
        <p className="ocupacion-leyenda-titulo">Forma: el estado de la reserva</p>
        <ul>
          {(['PENDIENTE', 'CONFIRMADA', 'EN_CURSO', 'FINALIZADA', 'NO_SHOW'] as const).map((estado) => (
            <li key={estado}>
              <span className={`etiqueta-${claseDeEstado(estado)}`}><FormaEstado estado={estado} /></span>
              {NOMBRE_DEL_ESTADO[estado]}
            </li>
          ))}
        </ul>
      </div>
      <div>
        <p className="ocupacion-leyenda-titulo">Otros</p>
        <ul>
          <li>
            <span className="ocupacion-muestra ocupacion-muestra-hoy" />
            Hoy
          </li>
          <li>
            <span className="ocupacion-muestra barra-bloqueo" />
            <Forma>{FORMA_BLOQUEO}</Forma>
            Bloqueo
          </li>
          <li>
            <span aria-hidden="true">›</span> Abre el detalle
          </li>
        </ul>
      </div>
    </div>
  )
  return (
    <>
      <section className="tarjeta ocupacion-leyenda-fija" aria-label="Leyenda">{contenido}</section>
      <details className="tarjeta ocupacion-leyenda-plegable">
        <summary>
          Leyenda <span className="ocupacion-leyenda-flecha" aria-hidden="true">›</span>
        </summary>
        {contenido}
      </details>
    </>
  )
}

/** El color de la barra: el de la cuenta o, si la API no la manda, el del estado. */
function claseDeBarra(reserva: ReservaOcupada): string {
  const cuenta = cuentaDe(reserva)
  return cuenta ? `cuenta-${cuenta}` : `barra-${claseDeEstado(reserva.estado)}`
}

/** «Ana Torres Vélez, del 12 nov al 15 nov, reserva RES-…, En curso. Con saldo: debe $ 675.000». */
function descripcionReserva(reserva: ReservaOcupada): string {
  const { codigo, estado, titularId, titularNombre, entrada, salida } = reserva
  const cuenta = textoDeCuenta(reserva)
  return `${titularNombre ?? `Titular ${titularId}`}, del ${fechaSinAnio(entrada)} al ${fechaSinAnio(salida)}, reserva ${codigo}, ${NOMBRE_DEL_ESTADO[estado] ?? estado}${cuenta ? `. ${cuenta}` : ''}`
}

function descripcionBloqueo(barra: Extract<Barra, { tipo: 'bloqueo' }>): string {
  const { motivo, fechaInicio, fechaFin } = barra.bloqueo
  return `Bloqueo${motivo ? `: ${motivo}` : ''}, del ${fechaCorta(fechaInicio)} al ${fechaCorta(fechaFin)}`
}

function Cuadricula({ filas, desde, hasta, hoy, alAbrir }: {
  filas: OcupacionApartamento[]
  desde: string
  hasta: string
  hoy: string
  alAbrir: (reserva: ReservaOcupada) => void
}) {
  const dias = useMemo(() => nochesDelRango(desde, hasta), [desde, hasta])
  const columnas = { '--noches': dias.length } as CSSProperties
  return (
    <div className="tarjeta tabla-marco ocupacion-cuadricula">
      <div className="desplazable" tabIndex={0} role="group" aria-label="Ocupación por noche">
        <div className="ocupacion-tablero" style={columnas}>
          <div className="ocupacion-cabecera" aria-hidden="true">
            <div className="ocupacion-celda-apartamento">Apartamento</div>
            <div className="ocupacion-dias">
              {dias.map((dia, indice) => (
                <div key={dia} className={`ocupacion-dia${dia === hoy ? ' ocupacion-dia-hoy' : ''}`}>
                  {diaDeLaSemana(dia)}
                  <b>{Number(dia.slice(8))}</b>
                  {/* El mes solo cuando cambia: es lo único que se necesita para ubicarse */}
                  <span className="ocupacion-mes">{indice === 0 || dia.endsWith('-01') ? fechaSinAnio(dia).split(' ')[1] : ' '}</span>
                </div>
              ))}
            </div>
          </div>
          <ul className="ocupacion-filas">
            {filas.map((fila) => (
              <FilaApartamento key={fila.apartamento} fila={fila} dias={dias} desde={desde} hasta={hasta} hoy={hoy} alAbrir={alAbrir} />
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function FilaApartamento({ fila, dias, desde, hasta, hoy, alAbrir }: {
  fila: OcupacionApartamento
  dias: string[]
  desde: string
  hasta: string
  alAbrir: (reserva: ReservaOcupada) => void
  hoy: string
}) {
  const barras = barrasDe(fila, desde, hasta)
  const carriles = Math.max(1, ...barras.map((barra) => barra.carril + 1))
  return (
    <li className="ocupacion-fila">
      <div className="ocupacion-celda-apartamento">
        <span className="codigo">{fila.apartamento}</span>
        <strong title={fila.nombre}>{fila.nombre}</strong>
        <EstadoApartamento estado={fila.estadoOperativo} />
        <MotivoFueraDeServicio motivo={fila.motivoFueraDeServicio} corto />
      </div>
      <div className="ocupacion-noches" style={{ '--carriles': carriles } as CSSProperties}>
        {dias.map((dia, indice) => (
          <div
            key={dia}
            className={`ocupacion-noche${dia === hoy ? ' ocupacion-noche-hoy' : ''}`}
            style={{ gridColumn: indice + 1 }}
          />
        ))}
        {barras.map((barra) => <BarraTablero key={barra.clave} barra={barra} alAbrir={alAbrir} />)}
      </div>
    </li>
  )
}

function BarraTablero({ barra, alAbrir }: { barra: Barra; alAbrir: (reserva: ReservaOcupada) => void }) {
  const posicion = {
    gridColumn: `${barra.columna + 1} / span ${barra.ancho}`,
    gridRow: barra.carril + 1,
  }
  const cortes = `${barra.cortadaInicio ? ' barra-corte-inicio' : ''}${barra.cortadaFin ? ' barra-corte-fin' : ''}`
  if (barra.tipo === 'bloqueo') {
    const descripcion = descripcionBloqueo(barra)
    return (
      <div className={`barra barra-bloqueo${cortes}`} style={posicion} title={descripcion} role="img" aria-label={descripcion}>
        <span className="barra-textos" aria-hidden="true">
          <span className="barra-titulo"><Forma>{FORMA_BLOQUEO}</Forma> Bloqueo</span>
          <span className="barra-detalle">{barra.bloqueo.motivo}</span>
        </span>
      </div>
    )
  }
  const { codigo, estado, titularId, titularNombre } = barra.reserva
  const descripcion = descripcionReserva(barra.reserva)
  return (
    <button
      type="button"
      className={`barra ${claseDeBarra(barra.reserva)} etiqueta-${claseDeEstado(estado)}${cortes}`}
      style={posicion}
      title={descripcion}
      aria-label={descripcion}
      aria-haspopup="dialog"
      onClick={() => alAbrir(barra.reserva)}
    >
      <span className="barra-textos">
        <span className="barra-titulo">
          <FormaEstado estado={estado} /> {titularNombre ?? <span className="codigo">{titularId}</span>}
        </span>
        <span className="barra-detalle">{textoCortoDeCuenta(barra.reserva) ?? codigo}</span>
      </span>
      {barra.ancho > 1 && <span className="barra-ir" aria-hidden="true">›</span>}
    </button>
  )
}

/** La primera entrada anterior a `desde` entre lo que sigue en curso o pendiente de salir: desde ahí se ve entera. */
function primeraEntradaAntesDe(filas: OcupacionApartamento[], desde: string): string | null {
  const entradas = filas.flatMap((fila) => fila.reservas)
    .filter((reserva) => reserva.estado === 'EN_CURSO' && reserva.entrada < desde)
    .map((reserva) => reserva.entrada)
    .sort()
  return entradas[0] ?? null
}

/** Lo básico de una reserva del tablero, sin salir de él (#23). */
function VentanaReserva({ reserva, alCerrar }: { reserva: ReservaOcupada; alCerrar: () => void }) {
  const cuenta = textoDeCuenta(reserva)
  return (
    <Dialogo titulo={reserva.titularNombre ?? reserva.titularId} subtitulo={<span className="codigo">{reserva.codigo}</span>} alCerrar={alCerrar}>
      <div className="dialogo-contenido">
        <div className="dialogo-cuerpo">
          <dl className="resumen-dialogo">
            <dt>Estado</dt>
            <dd><FormaEstado estado={reserva.estado} /> {NOMBRE_DEL_ESTADO[reserva.estado] ?? reserva.estado}</dd>
            <dt>Fechas</dt>
            <dd>
              {fechaCorta(reserva.entrada)} → {fechaCorta(reserva.salida)} · {cantidad(noches(reserva.entrada, reserva.salida), 'noche', 'noches')}
            </dd>
            <dt>Canal</dt>
            <dd>{NOMBRE_DEL_CANAL[reserva.canalOrigen] ?? reserva.canalOrigen}</dd>
            {cuenta && (
              <>
                <dt>Cuenta</dt>
                <dd>{cuenta}</dd>
              </>
            )}
          </dl>
        </div>
        <div className="dialogo-pie">
          <button type="button" className="boton boton-secundario" onClick={alCerrar}>Cerrar</button>
          <Link to={`/reservas/${encodeURIComponent(reserva.codigo)}`} className="boton" data-foco-inicial>
            Ver detalle completo <span aria-hidden="true">→</span>
          </Link>
        </div>
      </div>
    </Dialogo>
  )
}

/** En el celular, una cuadrícula quedaría ilegible: cada apartamento con la lista de lo que tiene. */
function ListaCelular({ filas, desde, hasta }: { filas: OcupacionApartamento[]; desde: string; hasta: string }) {
  return (
    <ul className="ocupacion-lista">
      {filas.map((fila) => {
        const barras = barrasDe(fila, desde, hasta)
        return (
          <li key={fila.apartamento} className="tarjeta ocupacion-lista-apartamento">
            <span className="codigo ocupacion-lista-codigo">{fila.apartamento}</span>
            <strong>{fila.nombre}</strong>
            <EstadoApartamento estado={fila.estadoOperativo} />
            <MotivoFueraDeServicio motivo={fila.motivoFueraDeServicio} />
            {barras.length === 0 && <p className="suave ocupacion-lista-vacia">Sin reservas ni bloqueos en este rango.</p>}
            {barras.map((barra) =>
              barra.tipo === 'reserva' ? (
                <Link
                  key={barra.clave}
                  to={`/reservas/${barra.reserva.codigo}`}
                  className={`ocupacion-item ${claseDeBarra(barra.reserva)} etiqueta-${claseDeEstado(barra.reserva.estado)}`}
                  aria-label={descripcionReserva(barra.reserva)}
                >
                  <FormaEstado estado={barra.reserva.estado} />
                  <span className="ocupacion-item-textos">
                    <strong>{barra.reserva.titularNombre ?? <span className="codigo">{barra.reserva.titularId}</span>}</strong>
                    <span>
                      {NOMBRE_DEL_ESTADO[barra.reserva.estado] ?? barra.reserva.estado}
                      {textoDeCuenta(barra.reserva) && ` · ${textoDeCuenta(barra.reserva)}`}
                    </span>
                    <span>
                      {fechaSinAnio(barra.reserva.entrada)} → {fechaSinAnio(barra.reserva.salida)} ·{' '}
                      <span className="codigo">{barra.reserva.codigo}</span>
                    </span>
                  </span>
                  <span aria-hidden="true">›</span>
                </Link>
              ) : (
                <div key={barra.clave} className="ocupacion-item barra-bloqueo">
                  <Forma>{FORMA_BLOQUEO}</Forma>
                  <span className="ocupacion-item-textos">
                    <span>Bloqueo · {fechaSinAnio(barra.bloqueo.fechaInicio)} → {fechaSinAnio(barra.bloqueo.fechaFin)}</span>
                    {barra.bloqueo.motivo && <span>{barra.bloqueo.motivo}</span>}
                  </span>
                </div>
              ),
            )}
          </li>
        )
      })}
    </ul>
  )
}

function Cargando() {
  return (
    <div className="tarjeta ocupacion-cargando" aria-busy="true">
      <p className="suave" role="status">Cargando la ocupación…</p>
      {Array.from({ length: 5 }, (_, fila) => (
        <span key={fila} className="fantasma" />
      ))}
    </div>
  )
}
