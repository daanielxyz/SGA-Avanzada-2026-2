import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { consultarOcupacion, type OcupacionApartamento } from '../api/ocupacion'
import { NOMBRE_DEL_ESTADO, type Pagina } from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import { FormaEstado } from '../componentes/EtiquetaEstado'
import { claseDeEstado } from '../componentes/estadosReserva'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, fechaCorta, fechaHoy, fechaSinAnio, noches, sumarDias } from '../formato'
import { barrasDe, nochesDelRango, type Barra } from '../ocupacion/barras'

const NOCHES_POR_DEFECTO = 14
const DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']

const ESTADO_OPERATIVO: Partial<Record<string, { nombre: string; forma: ReactNode }>> = {
  PREPARADO: { nombre: 'Preparado', forma: <path d="M2 6.5 4.8 9.5 10 3" /> },
  OCUPADO: { nombre: 'Ocupado', forma: <circle cx="6" cy="6" r="3" className="relleno" /> },
  PENDIENTE_PREPARACION: { nombre: 'Pendiente de preparación', forma: <path d="M6 1a5 5 0 0 1 0 10z" className="relleno" /> },
  EN_PREPARACION: { nombre: 'En preparación', forma: <path d="M6 2a4 4 0 1 1-4 4M6 3.5V6l1.8 1" /> },
  FUERA_DE_SERVICIO: { nombre: 'Fuera de servicio', forma: <path d="M6 1.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM2.8 2.8l6.4 6.4" /> },
}

const FORMA_BLOQUEO = <path d="M6 1.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM2.8 2.8l6.4 6.4" />

const esFecha = (valor: string | null): valor is string => Boolean(valor && /^\d{4}-\d{2}-\d{2}$/.test(valor))

function diaDeLaSemana(fecha: string): string {
  return DIAS[new Date(`${fecha}T00:00:00Z`).getUTCDay()]
}

type Resultado = { clave: string; pagina: Pagina<OcupacionApartamento> } | { clave: string; error: unknown }

/**
 * Tablero de ocupación (DEC-86). El rango vive en la dirección, para volver desde el detalle al mismo sitio. Se elige la
 * última noche que se quiere ver; a la API se le pide un día más, porque su `hasta` es exclusiva (EST-01).
 */
export default function Ocupacion() {
  useTitulo('Ocupación')
  const [parametros, setParametros] = useSearchParams()
  const hoy = fechaHoy()
  const desde = esFecha(parametros.get('desde')) ? (parametros.get('desde') as string) : hoy
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
        if (vigente) setResultado({ clave, pagina })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
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

  const moverRango = (dias: number) => cambiar({ desde: sumarDias(desde, dias), ultima: sumarDias(ultima, dias) })
  const irAHoy = () => cambiar({ desde: hoy, ultima: sumarDias(hoy, Math.max(totalNoches, 1) - 1) })

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
          <button type="button" className="boton boton-secundario" disabled={rangoInvalido} onClick={() => moverRango(-totalNoches)}>
            <span aria-hidden="true">←</span> Anterior
          </button>
          <button type="button" className="boton boton-secundario" onClick={irAHoy}>
            Hoy
          </button>
          <button type="button" className="boton boton-secundario" disabled={rangoInvalido} onClick={() => moverRango(totalNoches)}>
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
          <Cuadricula filas={actual.pagina.contenido} desde={desde} hasta={hasta} hoy={hoy} />
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

function EstadoOperativo({ estado }: { estado: string }) {
  const conocido = ESTADO_OPERATIVO[estado]
  return (
    <span className="ocupacion-estado">
      <Forma>{conocido?.forma ?? <circle cx="6" cy="6" r="2" className="relleno" />}</Forma>
      {conocido?.nombre ?? estado}
    </span>
  )
}

function Leyenda() {
  return (
    <ul className="ocupacion-leyenda" aria-label="Leyenda">
      {(['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'] as const).map((estado) => (
        <li key={estado}>
          <span className={`ocupacion-muestra barra-${claseDeEstado(estado)}`} />
          <span className={`etiqueta-${claseDeEstado(estado)}`}><FormaEstado estado={estado} /></span>
          {NOMBRE_DEL_ESTADO[estado]}
        </li>
      ))}
      <li>
        <span className="ocupacion-muestra barra-bloqueo" />
        <Forma>{FORMA_BLOQUEO}</Forma>
        Bloqueo
      </li>
      <li>
        <span className="ocupacion-muestra ocupacion-muestra-hoy" />
        Hoy
      </li>
      <li>
        <span aria-hidden="true">›</span> Abre el detalle
      </li>
    </ul>
  )
}

function descripcionReserva(barra: Extract<Barra, { tipo: 'reserva' }>): string {
  const { codigo, estado, titularId, entrada, salida } = barra.reserva
  return `Reserva ${codigo}, ${NOMBRE_DEL_ESTADO[estado] ?? estado}, titular ${titularId}, del ${fechaCorta(entrada)} al ${fechaCorta(salida)}`
}

function descripcionBloqueo(barra: Extract<Barra, { tipo: 'bloqueo' }>): string {
  const { motivo, fechaInicio, fechaFin } = barra.bloqueo
  return `Bloqueo${motivo ? `: ${motivo}` : ''}, del ${fechaCorta(fechaInicio)} al ${fechaCorta(fechaFin)}`
}

function Cuadricula({ filas, desde, hasta, hoy }: {
  filas: OcupacionApartamento[]
  desde: string
  hasta: string
  hoy: string
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
              <FilaApartamento key={fila.apartamento} fila={fila} dias={dias} desde={desde} hasta={hasta} hoy={hoy} />
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function FilaApartamento({ fila, dias, desde, hasta, hoy }: {
  fila: OcupacionApartamento
  dias: string[]
  desde: string
  hasta: string
  hoy: string
}) {
  const barras = barrasDe(fila, desde, hasta)
  const carriles = Math.max(1, ...barras.map((barra) => barra.carril + 1))
  return (
    <li className="ocupacion-fila">
      <div className="ocupacion-celda-apartamento">
        <span className="codigo">{fila.apartamento}</span>
        <strong title={fila.nombre}>{fila.nombre}</strong>
        <EstadoOperativo estado={fila.estadoOperativo} />
      </div>
      <div className="ocupacion-noches" style={{ '--carriles': carriles } as CSSProperties}>
        {dias.map((dia, indice) => (
          <div
            key={dia}
            className={`ocupacion-noche${dia === hoy ? ' ocupacion-noche-hoy' : ''}`}
            style={{ gridColumn: indice + 1 }}
          />
        ))}
        {barras.map((barra) => <BarraTablero key={barra.clave} barra={barra} />)}
      </div>
    </li>
  )
}

function BarraTablero({ barra }: { barra: Barra }) {
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
  const { codigo, estado, titularId } = barra.reserva
  const descripcion = descripcionReserva(barra)
  return (
    <Link
      to={`/reservas/${codigo}`}
      className={`barra barra-${claseDeEstado(estado)} etiqueta-${claseDeEstado(estado)}${cortes}`}
      style={posicion}
      title={descripcion}
      aria-label={descripcion}
    >
      <span className="barra-textos">
        <span className="barra-titulo"><FormaEstado estado={estado} /> <span className="codigo">{codigo}</span></span>
        <span className="barra-detalle codigo">{titularId}</span>
      </span>
      {barra.ancho > 1 && <span className="barra-ir" aria-hidden="true">›</span>}
    </Link>
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
            <EstadoOperativo estado={fila.estadoOperativo} />
            {barras.length === 0 && <p className="suave ocupacion-lista-vacia">Sin reservas ni bloqueos en este rango.</p>}
            {barras.map((barra) =>
              barra.tipo === 'reserva' ? (
                <Link
                  key={barra.clave}
                  to={`/reservas/${barra.reserva.codigo}`}
                  className={`ocupacion-item barra-${claseDeEstado(barra.reserva.estado)} etiqueta-${claseDeEstado(barra.reserva.estado)}`}
                >
                  <FormaEstado estado={barra.reserva.estado} />
                  <span className="ocupacion-item-textos">
                    <span><span className="codigo">{barra.reserva.codigo}</span> · {NOMBRE_DEL_ESTADO[barra.reserva.estado] ?? barra.reserva.estado}</span>
                    <span className="codigo">{barra.reserva.titularId}</span>
                    <span>{fechaSinAnio(barra.reserva.entrada)} → {fechaSinAnio(barra.reserva.salida)}</span>
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
