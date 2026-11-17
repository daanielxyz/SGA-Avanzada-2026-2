import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ErrorApi } from '../api/cliente'
import { consultarBitacora, listarCanales, type Canal, type EventoCanal } from '../api/canales'
import type { Pagina } from '../api/reservas'
import { errorDeRango, horaDe, mensajeLegible, NOMBRE_DEL_TIPO, rangoInicial, TEXTO_OPERACION } from '../canales/logica'
import { ConCodigos, EstadoCanal, ResultadoMensaje } from '../canales/Piezas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import CampoFecha from '../componentes/CampoFecha'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, FECHA_MAXIMA, fechaCorta } from '../formato'
import { errorParaMostrar } from '../reserva/mensajes'

type Consulta = { desde: string; hasta: string; pagina: number; intento: number }
type Resultado = { clave: string; pagina: Pagina<EventoCanal> } | { clave: string; error: unknown }

/** No hay un endpoint de un solo canal: se busca en la lista, que es corta (un canal por plataforma). */
async function buscarCanal(id: string): Promise<Canal | null> {
  for (let numero = 0; numero < 20; numero++) {
    const pagina = await listarCanales(numero)
    const canal = pagina.contenido.find((c) => c.id === id)
    if (canal) return canal
    if (numero + 1 >= pagina.totalPaginas) return null
  }
  return null
}

/**
 * La bitácora de un canal externo (sprint 5, solo administración): cada mensaje con el canal entre dos días, también
 * los rechazados, del más reciente al más antiguo y de 10 en 10 (CU-45 · BIT-01 · BIT-03). El mensaje del canal trae
 * datos del huésped: va plegado y como texto.
 */
export default function Bitacora() {
  const { id = '' } = useParams()
  const formulario = useId()
  const [canal, setCanal] = useState<Canal | null>(null)
  const [filtro, setFiltro] = useState(() => rangoInicial())
  const [errorFiltro, setErrorFiltro] = useState<ReturnType<typeof errorDeRango>>(null)
  const [consulta, setConsulta] = useState<Consulta>(() => ({ ...rangoInicial(), pagina: 0, intento: 0 }))
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const lista = useRef<HTMLParagraphElement>(null)
  // Al pasar de página la lista se desmonta mientras carga: el foco vuelve al resumen cuando llega la nueva
  const enfocarAlCargar = useRef(false)
  const clave = `${id}|${consulta.desde}|${consulta.hasta}|${consulta.pagina}|${consulta.intento}`
  useTitulo(canal ? `Bitácora de ${canal.nombre}` : 'Bitácora del canal')

  useEffect(() => {
    let vigente = true
    buscarCanal(id).then((encontrado) => vigente && setCanal(encontrado)).catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [id])

  useEffect(() => {
    let vigente = true
    consultarBitacora(id, consulta.desde, consulta.hasta, consulta.pagina)
      .then((pagina) => vigente && setResultado({ clave, pagina }))
      .catch((error: unknown) => vigente && setResultado({ clave, error }))
    return () => {
      vigente = false
    }
  }, [id, consulta, clave])

  const actual = resultado?.clave === clave ? resultado : null
  const pagina = actual && 'pagina' in actual ? actual.pagina : null

  useEffect(() => {
    if (pagina && enfocarAlCargar.current) {
      enfocarAlCargar.current = false
      lista.current?.focus()
    }
  }, [pagina])
  const noExiste = actual && 'error' in actual && actual.error instanceof ErrorApi && actual.error.estado === 404
  const volver = <Link to="/canales" className="volver"><span aria-hidden="true">←</span> Canales de venta</Link>

  function consultar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const error = errorDeRango(filtro.desde, filtro.hasta)
    setErrorFiltro(error)
    if (error) {
      document.getElementById(`${formulario}-${error.campo}`)?.focus()
      return
    }
    setConsulta({ ...filtro, pagina: 0, intento: consulta.intento + 1 })
  }

  if (noExiste) {
    return (
      <div className="bitacora">
        <div className="bitacora-encabezado">{volver}</div>
        <BloqueVacio encabezado="h1" icono="noEncontrada" titulo="Este canal no existe">
          Puede que la dirección esté mal escrita. Vuelve a Canales de venta y elige uno de la lista.
        </BloqueVacio>
      </div>
    )
  }

  const campoFecha = (campo: 'desde' | 'hasta', rotulo: string) => {
    const error = errorFiltro?.campo === campo ? errorFiltro.mensaje : undefined
    return (
      <div className="campo">
        <label className="campo-rotulo" htmlFor={`${formulario}-${campo}`}>{rotulo}</label>
        <CampoFecha etiqueta={rotulo} id={`${formulario}-${campo}`} className="campo-control" max={FECHA_MAXIMA}
          value={filtro[campo]} aria-invalid={Boolean(error)}
          aria-describedby={error ? `${formulario}-${campo}-error` : undefined}
          onChange={(e) => {
            setFiltro({ ...filtro, [campo]: e.target.value })
            setErrorFiltro(null)
          }} />
        {error && <p className="campo-mensaje" id={`${formulario}-${campo}-error`}>{error}</p>}
      </div>
    )
  }

  return (
    <div className="bitacora">
      <div className="bitacora-encabezado">
        {volver}
        <h1 tabIndex={-1}>{canal ? <>Bitácora de <span className="texto-largo">{canal.nombre}</span></> : 'Bitácora del canal'}</h1>
        {canal && (
          <p className="bitacora-canal">
            {NOMBRE_DEL_TIPO[canal.tipo]}<span aria-hidden="true">·</span><EstadoCanal canal={canal} />
          </p>
        )}
      </div>
      {canal && !canal.activo && (
        <Aviso tipo="info" mensaje={`${canal.nombre} está inactivo: no crea reservas nuevas. Lo que mande igual queda aquí.`} />
      )}

      <form className="tarjeta bitacora-filtros" aria-label="Fechas de la bitácora" onSubmit={consultar} noValidate>
        {campoFecha('desde', 'Desde')}
        {campoFecha('hasta', 'Hasta')}
        <button type="submit" className="boton">Consultar</button>
        <p className="campo-pista">Al abrir, los últimos 7 días. Las dos fechas se incluyen.</p>
      </form>

      {!actual && (
        <div className="tarjeta eventos-cargando" aria-busy="true">
          {Array.from({ length: 4 }, (_, fila) => (
            <div key={fila} className="fila-fantasma">
              {Array.from({ length: 4 }, (__, celda) => <span key={celda} className="fantasma" />)}
            </div>
          ))}
          <p className="carga-texto suave" role="status">Cargando la bitácora…</p>
        </div>
      )}
      {actual && 'error' in actual && !noExiste && (
        <Aviso tipo="error" asunto="No pudimos cargar la bitácora" mensaje={errorParaMostrar(actual.error).mensaje}
          accion={
            <button type="button" className="boton boton-secundario boton-chico"
              onClick={() => setConsulta({ ...consulta, intento: consulta.intento + 1 })}>
              Reintentar
            </button>
          } />
      )}
      {pagina && pagina.totalElementos === 0 && (
        <BloqueVacio icono="canales" titulo="Sin mensajes en esas fechas">
          {`Entre el ${fechaCorta(consulta.desde)} y el ${fechaCorta(consulta.hasta)} el canal no mandó nada. Prueba con otras fechas.`}
        </BloqueVacio>
      )}
      {pagina && pagina.totalElementos > 0 && (
        <>
          <p className="bitacora-resumen" ref={lista} tabIndex={-1}>
            {cantidad(pagina.totalElementos, 'mensaje', 'mensajes')} del {fechaCorta(consulta.desde)} al{' '}
            {fechaCorta(consulta.hasta)}, del más reciente al más antiguo.
          </p>
          <div className="eventos-marco">
            <div className="eventos-columnas" aria-hidden="true">
              <span>Fecha y hora</span><span>Operación</span><span>Resultado</span><span>Detalle</span>
            </div>
            <ol className="eventos" aria-label={canal ? `Mensajes con ${canal.nombre}` : 'Mensajes con el canal'}>
              {pagina.contenido.map((evento) => <Evento key={evento.id} evento={evento} />)}
            </ol>
          </div>
          <Paginador numero={pagina.numero} totalPaginas={pagina.totalPaginas} totalElementos={pagina.totalElementos}
            unidad={['mensaje', 'mensajes']} alCambiar={(n) => {
              enfocarAlCargar.current = true
              setConsulta({ ...consulta, pagina: n })
            }} />
        </>
      )}
    </div>
  )
}

function Evento({ evento }: { evento: EventoCanal }) {
  const operacion = TEXTO_OPERACION[evento.operacion]
  const recibido = evento.sentido === 'ENTRANTE'
  return (
    <li className="evento">
      <dl className="evento-datos">
        <div className="evento-fecha">
          <dt className="solo-lectores">Fecha y hora</dt>
          <dd>
            <time dateTime={evento.fechaHora}>{fechaCorta(evento.fechaHora)}</time>
            <span className="evento-fecha-separador"> · </span>
            <time dateTime={evento.fechaHora}>{horaDe(evento.fechaHora)}</time>
          </dd>
        </div>
        <div className="evento-operacion">
          <dt className="solo-lectores">Operación</dt>
          <dd>
            <strong>{operacion}</strong>
            <span className="evento-sentido">
              <svg className="icono" viewBox="0 0 24 24" aria-hidden="true">
                <path d={recibido ? 'M12 4v16M6 14l6 6 6-6' : 'M12 20V4M6 10l6-6 6 6'} />
              </svg>
              {recibido ? 'Recibido del canal' : 'Enviado al canal'}
            </span>
          </dd>
        </div>
        <div className="evento-resultado">
          <dt className="solo-lectores">Resultado</dt>
          <dd><ResultadoMensaje resultado={evento.resultado} /></dd>
        </div>
        <div className="evento-detalle">
          <dt className="solo-lectores">Detalle</dt>
          <dd>{evento.detalle ? <ConCodigos texto={evento.detalle} /> : <span className="suave">—</span>}</dd>
        </div>
      </dl>
      <details className="mensaje-canal">
        <summary><span className="mensaje-canal-flecha" aria-hidden="true">▸</span>Ver mensaje del canal</summary>
        <pre className="mensaje-canal-texto" tabIndex={0}
          aria-label={`Mensaje del canal, ${operacion.toLowerCase()} del ${fechaCorta(evento.fechaHora)}`}>
          {mensajeLegible(evento.cargaUtil)}
        </pre>
      </details>
    </li>
  )
}
