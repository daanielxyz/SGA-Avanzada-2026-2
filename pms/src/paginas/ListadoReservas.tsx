import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import {
  listarReservas,
  NOMBRE_DEL_CANAL,
  NOMBRE_DEL_ESTADO,
  type CanalOrigen,
  type EstadoReserva,
  type Pagina,
  type Reserva,
} from '../api/reservas'
import Aviso from '../componentes/Aviso'
import BloqueVacio from '../componentes/BloqueVacio'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import Icono from '../componentes/Icono'
import Paginador from '../componentes/Paginador'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, dinero, fechaCorta, noches } from '../formato'

const FILTROS = ['desde', 'hasta', 'estado', 'apartamento', 'canal', 'titular'] as const
type Filtro = (typeof FILTROS)[number]
type ValoresFiltro = Record<Filtro, string>

const ROTULOS: Record<Filtro, string> = {
  desde: 'Desde',
  hasta: 'Hasta',
  estado: 'Estado',
  apartamento: 'Apartamento',
  canal: 'Canal',
  titular: 'Titular (id)',
}

/** El valor de un filtro como lo lee la persona. */
function textoDelFiltro(filtro: Filtro, valor: string): string {
  if (filtro === 'desde' || filtro === 'hasta') return fechaCorta(valor)
  if (filtro === 'estado') return NOMBRE_DEL_ESTADO[valor as EstadoReserva] ?? valor
  if (filtro === 'canal') return NOMBRE_DEL_CANAL[valor as CanalOrigen] ?? valor
  return valor
}

function valoresDe(parametros: URLSearchParams): ValoresFiltro {
  return Object.fromEntries(FILTROS.map((filtro) => [filtro, parametros.get(filtro) ?? ''])) as ValoresFiltro
}

function parametrosDe(valores: ValoresFiltro): URLSearchParams {
  return new URLSearchParams(Object.entries(valores).filter(([, valor]) => valor.trim() !== ''))
}

type Resultado = { clave: string; pagina: Pagina<Reserva> } | { clave: string; error: unknown }

// Dónde iba la persona al abrir una reserva, para dejarla ahí mismo al volver al listado
let posicionAlSalir: { busqueda: string; y: number } | null = null

/** Listado de reservas de recepción (CU-25 · DEC-72). Los filtros y la página viven en la dirección de la pantalla. */
export default function ListadoReservas() {
  const [parametros, setParametros] = useSearchParams()
  const { search } = useLocation()
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const consulta = parametros.toString()
  const clave = `${consulta}#${intento}`
  useTitulo('Reservas')

  useEffect(() => {
    let vigente = true
    listarReservas(consulta)
      .then((pagina) => {
        if (vigente) setResultado({ clave, pagina })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
  }, [consulta, clave])

  const valores = valoresDe(parametros)
  const hayFiltros = Object.values(valores).some(Boolean)
  const actual = resultado?.clave === clave ? resultado : null
  const error = actual && 'error' in actual ? actual.error : null
  // Un filtro mal puesto se explica junto a los filtros; lo demás, en lugar de la tabla
  const errorDeFiltro = error instanceof ErrorApi && (error.estado === 400 || error.estado === 409) ? error : null

  const limpiar = () => setParametros(new URLSearchParams())
  const quitar = (filtro: Filtro) => {
    const siguientes = parametrosDe({ ...valores, [filtro]: '' })
    setParametros(siguientes)
  }
  const irAPagina = (pagina: number) => {
    const siguientes = new URLSearchParams(parametros)
    siguientes.set('pagina', String(pagina))
    setParametros(siguientes)
  }

  return (
    <div className="reservas">
      <div className="reservas-titulo">
        <h1>Reservas</h1>
        <IrALaReserva />
      </div>

      {/* Al cambiar los filtros aplicados, el formulario vuelve a empezar desde ellos */}
      <Filtros
        key={parametrosDe(valores).toString()}
        valores={valores}
        errorDeFiltro={errorDeFiltro}
        alAplicar={(nuevos) => setParametros(parametrosDe(nuevos))}
        alLimpiar={limpiar}
        alQuitar={quitar}
      />

      {!actual && <Cargando />}
      {error !== null && !errorDeFiltro && (
        <Aviso
          tipo="error"
          asunto="No pudimos cargar las reservas"
          mensaje={error instanceof ErrorApi ? error.message : 'Revisa tu conexión e inténtalo de nuevo.'}
          accion={
            <button type="button" className="boton boton-secundario boton-chico" onClick={() => setIntento(intento + 1)}>
              Reintentar
            </button>
          }
        />
      )}
      {actual && 'pagina' in actual && (
        <Resultados
          pagina={actual.pagina}
          hayFiltros={hayFiltros}
          volverA={search}
          alLimpiar={limpiar}
          alCambiarPagina={irAPagina}
        />
      )}
    </div>
  )
}

/** Abre una reserva directo por su código: el listado no busca por código (DEC-72). */
function IrALaReserva() {
  const navegar = useNavigate()
  const [codigo, setCodigo] = useState('')
  const [falta, setFalta] = useState(false)

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const limpio = codigo.trim().toUpperCase()
    setFalta(!limpio)
    if (limpio) navegar(`/reservas/${encodeURIComponent(limpio)}`)
  }

  return (
    <form className="ir-a-reserva" onSubmit={enviar} noValidate>
      <div>
        <label className="campo-rotulo" htmlFor="ir-a-reserva">
          Ir a la reserva
        </label>
        <input
          id="ir-a-reserva"
          className="campo-control codigo"
          type="text"
          placeholder="RES-2026-00042"
          autoComplete="off"
          value={codigo}
          aria-invalid={falta}
          aria-describedby={falta ? 'ir-a-reserva-falta' : undefined}
          onChange={(e) => {
            setCodigo(e.target.value)
            setFalta(false)
          }}
        />
        {falta && (
          <p id="ir-a-reserva-falta" className="campo-mensaje">
            Escribe el código de la reserva.
          </p>
        )}
      </div>
      <button type="submit" className="boton boton-secundario">
        Ir
      </button>
    </form>
  )
}

function Filtros({ valores, errorDeFiltro, alAplicar, alLimpiar, alQuitar }: {
  valores: ValoresFiltro
  errorDeFiltro: ErrorApi | null
  alAplicar: (valores: ValoresFiltro) => void
  alLimpiar: () => void
  alQuitar: (filtro: Filtro) => void
}) {
  const [borrador, setBorrador] = useState(valores)
  // Mientras la persona no los pliegue o despliegue, en el celular solo se abren si hay un error de filtro: si no,
  // no se verían ni el error ni el campo por corregir
  const [eleccion, setEleccion] = useState<boolean | null>(null)
  const abiertos = eleccion ?? Boolean(errorDeFiltro)
  const [apartamentos, setApartamentos] = useState<string[]>([])
  const activos = FILTROS.filter((filtro) => valores[filtro])
  const rangoInvalido = errorDeFiltro?.estado === 409 && Boolean(valores.desde && valores.hasta)

  // Si la lista no carga, el filtro queda solo con «Todos» y el resto de la pantalla sigue funcionando
  useEffect(() => {
    let vigente = true
    listarCodigosDeApartamentos()
      .then((codigos) => {
        if (vigente) setApartamentos(codigos)
      })
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  const cambiar = (filtro: Filtro) => (evento: { target: { value: string } }) =>
    setBorrador({ ...borrador, [filtro]: evento.target.value })

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    alAplicar(borrador)
  }

  const opcionesApartamento = valores.apartamento && !apartamentos.includes(valores.apartamento)
    ? [valores.apartamento, ...apartamentos]
    : apartamentos

  return (
    <div className="filtros">
      <button
        type="button"
        className="boton boton-secundario boton-bloque boton-filtros"
        aria-expanded={abiertos}
        aria-controls="filtros-reservas"
        onClick={() => setEleccion(!abiertos)}
      >
        <span>Filtros{activos.length > 0 && ` (${activos.length})`}</span>
        <Icono nombre="abajo" />
      </button>

      {activos.length > 0 && (
        <ul className="filtros-activos" aria-label="Filtros aplicados">
          {activos.map((filtro) => {
            const texto = `${ROTULOS[filtro].replace(' (id)', '')}: ${textoDelFiltro(filtro, valores[filtro])}`
            return (
              <li key={filtro}>
                <button
                  type="button"
                  className="etiqueta filtro-activo"
                  aria-label={`Quitar filtro ${texto}`}
                  onClick={() => alQuitar(filtro)}
                >
                  {texto}
                  <Icono nombre="cerrar" />
                </button>
              </li>
            )
          })}
          <li className="filtros-activos-limpiar">
            <button type="button" className="enlace" onClick={alLimpiar}>
              Limpiar
            </button>
          </li>
        </ul>
      )}

      <form
        id="filtros-reservas"
        role="search"
        aria-label="Filtros"
        className={abiertos ? 'tarjeta filtros-cuerpo abierto' : 'tarjeta filtros-cuerpo'}
        onSubmit={enviar}
        noValidate
      >
        <div className="filtros-campos">
          <div>
            <label className="campo-rotulo" htmlFor="filtro-desde">
              {ROTULOS.desde}
            </label>
            <input
              id="filtro-desde"
              className="campo-control"
              type="date"
              value={borrador.desde}
              aria-invalid={rangoInvalido}
              aria-describedby={rangoInvalido ? 'filtros-error' : undefined}
              onChange={cambiar('desde')}
            />
          </div>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-hasta">
              {ROTULOS.hasta}
            </label>
            <input
              id="filtro-hasta"
              className="campo-control"
              type="date"
              value={borrador.hasta}
              onChange={cambiar('hasta')}
            />
          </div>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-estado">
              {ROTULOS.estado}
            </label>
            <select id="filtro-estado" className="campo-control" value={borrador.estado} onChange={cambiar('estado')}>
              <option value="">Todos</option>
              {Object.entries(NOMBRE_DEL_ESTADO).map(([valor, nombre]) => (
                <option key={valor} value={valor}>
                  {nombre}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-apartamento">
              {ROTULOS.apartamento}
            </label>
            <select
              id="filtro-apartamento"
              className="campo-control"
              value={borrador.apartamento}
              onChange={cambiar('apartamento')}
            >
              <option value="">Todos</option>
              {opcionesApartamento.map((codigo) => (
                <option key={codigo} value={codigo}>
                  {codigo}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-canal">
              {ROTULOS.canal}
            </label>
            <select id="filtro-canal" className="campo-control" value={borrador.canal} onChange={cambiar('canal')}>
              <option value="">Todos</option>
              {Object.entries(NOMBRE_DEL_CANAL).map(([valor, nombre]) => (
                <option key={valor} value={valor}>
                  {nombre}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-titular">
              {ROTULOS.titular}
            </label>
            <input
              id="filtro-titular"
              className="campo-control codigo"
              type="text"
              autoComplete="off"
              value={borrador.titular}
              onChange={cambiar('titular')}
            />
          </div>
          <div className="filtros-botones">
            <button type="button" className="boton boton-secundario" onClick={alLimpiar}>
              Limpiar
            </button>
            <button type="submit" className="boton">
              Filtrar
            </button>
          </div>
        </div>

        {errorDeFiltro && (
          <div id="filtros-error" className="filtros-error">
            <Aviso
              tipo="error"
              asunto="No se pudo aplicar el filtro"
              mensaje={errorDeFiltro.message}
              detalles={errorDeFiltro.detalles}
            />
          </div>
        )}
      </form>
    </div>
  )
}

function Cargando() {
  return (
    <div className="tarjeta" aria-busy="true">
      {Array.from({ length: 5 }, (_, fila) => (
        <div key={fila} className="fila-fantasma">
          {Array.from({ length: 6 }, (__, celda) => (
            <span key={celda} className="fantasma" />
          ))}
        </div>
      ))}
      <p className="carga-texto suave" role="status">
        Cargando reservas…
      </p>
    </div>
  )
}

function Resultados({ pagina, hayFiltros, volverA, alLimpiar, alCambiarPagina }: {
  pagina: Pagina<Reserva>
  hayFiltros: boolean
  volverA: string
  alLimpiar: () => void
  alCambiarPagina: (pagina: number) => void
}) {
  useEffect(() => {
    if (posicionAlSalir?.busqueda === volverA && posicionAlSalir.y > 0) window.scrollTo(0, posicionAlSalir.y)
    posicionAlSalir = null
  }, [volverA])
  const recordarPosicion = () => {
    posicionAlSalir = { busqueda: volverA, y: window.scrollY }
  }

  if (pagina.contenido.length === 0) {
    if (pagina.numero > 0) {
      return (
        <BloqueVacio
          icono="buscar"
          titulo="Esta página ya no tiene reservas"
          accion={
            <button type="button" className="boton boton-secundario" onClick={() => alCambiarPagina(0)}>
              Ir a la primera página
            </button>
          }
        >
          El listado es más corto de lo que era.
        </BloqueVacio>
      )
    }
    if (hayFiltros) {
      return (
        <BloqueVacio
          icono="buscar"
          titulo="Ninguna reserva coincide con los filtros"
          accion={
            <button type="button" className="boton boton-secundario" onClick={alLimpiar}>
              Limpiar filtros
            </button>
          }
        >
          Prueba con otro estado o amplía el rango de fechas.
        </BloqueVacio>
      )
    }
    return (
      <BloqueVacio icono="reservas" titulo="Todavía no hay reservas">
        Cuando se registre la primera reserva de este alojamiento, aparecerá aquí.
      </BloqueVacio>
    )
  }

  return (
    <>
      {/* En pantallas anchas, tabla densa; en el celular, una tarjeta por reserva */}
      <section aria-label="Resultados" className="tarjeta reservas-tabla">
        <div className="desplazable">
          <table className="tabla tabla-reservas">
            <caption className="solo-lectores">Reservas</caption>
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Apto.</th>
                <th scope="col">Titular</th>
                <th scope="col">Entrada y salida</th>
                <th scope="col" className="derecha">
                  Noches
                </th>
                <th scope="col">Estado</th>
                <th scope="col">Canal</th>
                <th scope="col" className="derecha">
                  Ocup.
                </th>
                <th scope="col" className="derecha">
                  Total
                </th>
              </tr>
            </thead>
            <tbody>
              {pagina.contenido.map((reserva) => (
                <tr key={reserva.codigo}>
                  <td>
                    <Link
                      to={`/reservas/${reserva.codigo}`}
                      state={{ volverA }}
                      className="codigo enlace-codigo"
                      onClick={recordarPosicion}
                    >
                      {reserva.codigo}
                    </Link>
                  </td>
                  <td className="codigo">
                    <span className="apartamento-recortado" title={reserva.apartamento}>
                      {reserva.apartamento}
                    </span>
                  </td>
                  <td className="celda-titular">
                    <Titular reserva={reserva} />
                  </td>
                  <td>
                    <span className="estancia">{fechaCorta(reserva.entrada)}</span>
                    <span className="estancia suave">{fechaCorta(reserva.salida)}</span>
                  </td>
                  <td className="derecha numero">{noches(reserva.entrada, reserva.salida)}</td>
                  <td>
                    <EtiquetaEstado estado={reserva.estado} />
                  </td>
                  <td>{NOMBRE_DEL_CANAL[reserva.canalOrigen]}</td>
                  <td className="derecha numero">{reserva.ocupantes.length}</td>
                  <td className="derecha codigo importe">{dinero(reserva.valor.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <ul className="reservas-tarjetas" aria-label="Reservas">
        {pagina.contenido.map((reserva) => {
          const totalNoches = noches(reserva.entrada, reserva.salida)
          return (
            <li key={reserva.codigo}>
              <Link
                to={`/reservas/${reserva.codigo}`}
                state={{ volverA }}
                className="reserva-tarjeta"
                onClick={recordarPosicion}
              >
                <span className="reserva-tarjeta-fila">
                  <span className="codigo reserva-tarjeta-codigo">{reserva.codigo}</span>
                  <EtiquetaEstado estado={reserva.estado} />
                </span>
                <span className="reserva-tarjeta-linea">
                  <span className="codigo">{reserva.apartamento}</span> ·{' '}
                  {reserva.titularNombre ?? <span className="codigo">{reserva.titularId}</span>}
                </span>
                <span className="reserva-tarjeta-linea suave">
                  {fechaCorta(reserva.entrada)} → {fechaCorta(reserva.salida)} ·{' '}
                  {cantidad(totalNoches, 'noche', 'noches')}
                </span>
                <span className="reserva-tarjeta-fila reserva-tarjeta-pie">
                  <span className="suave">
                    {NOMBRE_DEL_CANAL[reserva.canalOrigen]} ·{' '}
                    {cantidad(reserva.ocupantes.length, 'ocupante', 'ocupantes')}
                  </span>
                  <span className="codigo importe">{dinero(reserva.valor.total)}</span>
                </span>
              </Link>
            </li>
          )
        })}
      </ul>

      <Paginador
        numero={pagina.numero}
        totalPaginas={pagina.totalPaginas}
        totalElementos={pagina.totalElementos}
        alCambiar={alCambiarPagina}
      />
    </>
  )
}

/** El nombre, que es lo que recepción reconoce, con el id debajo para no confundir a dos personas iguales. */
function Titular({ reserva }: { reserva: Reserva }) {
  return (
    <>
      {reserva.titularNombre && (
        <span className="titular-nombre" title={reserva.titularNombre}>
          {reserva.titularNombre}
        </span>
      )}
      <span className="codigo titular-id" title={reserva.titularId}>
        {reserva.titularId}
      </span>
    </>
  )
}
