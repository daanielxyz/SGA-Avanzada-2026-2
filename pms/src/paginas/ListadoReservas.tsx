import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { listarCodigosDeApartamentos } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import {
  consultarReserva,
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
import CampoFecha from '../componentes/CampoFecha'
import EtiquetaEstado from '../componentes/EtiquetaEstado'
import Icono from '../componentes/Icono'
import { textoDeEleccion, type Atajo } from '../componentes/eleccion'
import Paginador from '../componentes/Paginador'
import SeleccionVarios from '../componentes/SeleccionVarios'
import { usePulso } from '../componentes/usePulso'
import { useTitulo } from '../componentes/useTitulo'
import { cantidad, dinero, FECHA_MAXIMA, fechaCorta, noches } from '../formato'
import { totalConIva } from '../reserva/valor'
import EnviosTraConError from '../reserva/EnviosTraConError'

const FILTROS = ['codigo', 'desde', 'hasta', 'estado', 'apartamento', 'canal', 'titular'] as const
type Filtro = (typeof FILTROS)[number]
type ValoresFiltro = Record<Filtro, string>

const ROTULOS: Record<Filtro, string> = {
  codigo: 'Código',
  desde: 'Desde',
  hasta: 'Hasta',
  estado: 'Estado',
  apartamento: 'Apartamento',
  canal: 'Canal',
  titular: 'Titular (id)',
}

// DEC-119: estos tres admiten varios valores, separados por comas en la dirección y en la consulta a la API
const VARIOS = ['estado', 'apartamento', 'canal'] as const
type FiltroVarios = (typeof VARIOS)[number]

const OPCIONES_ESTADO = Object.entries(NOMBRE_DEL_ESTADO) as [EstadoReserva, string][]
const OPCIONES_CANAL = Object.entries(NOMBRE_DEL_CANAL) as [CanalOrigen, string][]
/** Las que todavía ocupan noches, que es lo que recepción más busca, y las que ya terminaron. */
const ATAJOS_ESTADO: readonly Atajo[] = [
  { nombre: 'Activas', valores: ['PENDIENTE', 'CONFIRMADA', 'EN_CURSO'] },
  { nombre: 'Terminadas', valores: ['FINALIZADA', 'CANCELADA', 'NO_SHOW'] },
]

function esVarios(filtro: Filtro): filtro is FiltroVarios {
  return (VARIOS as readonly string[]).includes(filtro)
}

/** «PENDIENTE,CONFIRMADA» → los dos, sin vacíos ni repetidos. */
function listaDe(valor: string): string[] {
  return [...new Set(valor.split(',').map((v) => v.trim()).filter(Boolean))]
}

/** El valor de un filtro como lo lee la persona. */
function textoDelFiltro(filtro: Filtro, valor: string): string {
  if (filtro === 'desde' || filtro === 'hasta') return fechaCorta(valor)
  if (filtro === 'estado') return textoDeEleccion(listaDe(valor), OPCIONES_ESTADO, ATAJOS_ESTADO)
  if (filtro === 'canal') return textoDeEleccion(listaDe(valor), OPCIONES_CANAL)
  if (filtro === 'apartamento') return listaDe(valor).join(', ')
  return valor
}

// Una dirección vieja o pegada puede repetir el parámetro (estado=A&estado=B): se juntan con comas
function valoresDe(parametros: URLSearchParams): ValoresFiltro {
  return Object.fromEntries(FILTROS.map((filtro) => [filtro, esVarios(filtro)
    ? listaDe(parametros.getAll(filtro).join(',')).join(',')
    : parametros.get(filtro) ?? ''])) as ValoresFiltro
}

function parametrosDe(valores: ValoresFiltro): URLSearchParams {
  return new URLSearchParams(Object.entries(valores).filter(([, valor]) => valor.trim() !== ''))
}

type Resultado = { clave: string; pagina: Pagina<Reserva> } | { clave: string; error: unknown }

/** Los parámetros que entiende `GET /api/reservas`: todos menos el código, que el listado del backend no filtra. */
function consultaAlListado(parametros: URLSearchParams): string {
  const sinCodigo = new URLSearchParams(parametros)
  sinCodigo.delete('codigo')
  return sinCodigo.toString()
}

/** La reserva que se buscó por código, con la forma de una página para mostrarla en la misma tabla. */
function paginaDeUna(reserva: Reserva): Pagina<Reserva> {
  return { contenido: [reserva], numero: 0, tamano: 1, totalElementos: 1, totalPaginas: 1 }
}

// Dónde iba la persona al abrir una reserva, para dejarla ahí mismo al volver al listado
let posicionAlSalir: { busqueda: string; y: number } | null = null

/**
 * Listado de reservas de recepción (CU-25 · DEC-72). Los filtros y la página viven en la dirección de la pantalla.
 * El código es un filtro más, pero el listado del backend no lo tiene: con un código se pide esa reserva
 * (`GET /api/reservas/{codigo}`) y se muestra sola en la misma tabla; mientras esté, los demás filtros no se aplican.
 */
export default function ListadoReservas() {
  const [parametros, setParametros] = useSearchParams()
  const { search } = useLocation()
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const consulta = parametros.toString()
  const clave = `${consulta}#${intento}`
  const valores = valoresDe(parametros)
  const codigo = valores.codigo.trim().toUpperCase()
  const consultaListado = consultaAlListado(parametros)
  useTitulo('Reservas')
  // Las reservas que entran por la web o por otra persona aparecen solas, sin recargar la página
  const pulso = usePulso()

  useEffect(() => {
    let vigente = true
    const pedido = codigo ? consultarReserva(codigo).then(paginaDeUna) : listarReservas(consultaListado)
    pedido
      .then((pagina) => {
        if (vigente) setResultado({ clave, pagina })
      })
      .catch((error: unknown) => {
        // Si falla una relectura de fondo, la lista que ya se veía se queda: el error solo reemplaza a la carga
        if (vigente) setResultado((previo) => (previo?.clave === clave && 'pagina' in previo ? previo : { clave, error }))
      })
    return () => {
      vigente = false
    }
  }, [codigo, consultaListado, clave, pulso])

  const hayFiltros = Object.values(valores).some(Boolean)
  const actual = resultado?.clave === clave ? resultado : null
  const error = actual && 'error' in actual ? actual.error : null
  // Un filtro mal puesto se explica junto a los filtros; lo demás, en lugar de la tabla
  const errorDeFiltro = error instanceof ErrorApi && (error.estado === 400 || error.estado === 409) ? error : null
  // Un código que no existe no es un fallo de la pantalla: se dice en el lugar de la lista
  const noExiste = Boolean(codigo) && error instanceof ErrorApi && error.estado === 404

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
        <Link to="/reservas/nueva" className="boton">
          <span aria-hidden="true">+</span> Nueva reserva
        </Link>
      </div>

      <EnviosTraConError />

      {/* Al cambiar los filtros aplicados, el formulario vuelve a empezar desde ellos */}
      <Filtros
        key={parametrosDe(valores).toString()}
        valores={valores}
        errorDeFiltro={errorDeFiltro}
        alAplicar={(nuevos) => setParametros(parametrosDe({ ...nuevos, codigo: nuevos.codigo.trim().toUpperCase() }))}
        alLimpiar={limpiar}
        alQuitar={quitar}
      />

      {!actual && <Cargando />}
      {noExiste && (
        <BloqueVacio
          icono="buscar"
          titulo={`No existe la reserva ${codigo}`}
          accion={
            <button type="button" className="boton boton-secundario" onClick={() => quitar('codigo')}>
              Quitar el código
            </button>
          }
        >
          Revisa que esté completo, como RES-2026-00042.
        </BloqueVacio>
      )}
      {error !== null && !errorDeFiltro && !noExiste && (
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
  const elegir = (filtro: FiltroVarios) => (elegidos: string[]) => setBorrador({ ...borrador, [filtro]: elegidos.join(',') })

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    alAplicar(borrador)
  }

  // Un apartamento de la dirección que ya no está en la lista se conserva como opción
  const opcionesApartamento = [...listaDe(valores.apartamento).filter((codigo) => !apartamentos.includes(codigo)), ...apartamentos]

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
            <label className="campo-rotulo" htmlFor="filtro-codigo">
              {ROTULOS.codigo}
            </label>
            <input
              id="filtro-codigo"
              className="campo-control codigo"
              type="text"
              placeholder="RES-2026-00042"
              autoComplete="off"
              autoCapitalize="characters"
              spellCheck={false}
              maxLength={40}
              value={borrador.codigo}
              aria-describedby="filtro-codigo-pista"
              onChange={cambiar('codigo')}
            />
          </div>
          <p id="filtro-codigo-pista" className="campo-pista filtros-codigo-pista">
            Con un código se muestra solo esa reserva: los demás filtros no se aplican hasta que lo quites.
          </p>
          <div>
            <label className="campo-rotulo" htmlFor="filtro-desde">
              {ROTULOS.desde}
            </label>
            <CampoFecha
              etiqueta={ROTULOS.desde}
              id="filtro-desde"
              className="campo-control"
              max={FECHA_MAXIMA}
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
            <CampoFecha
              etiqueta={ROTULOS.hasta}
              id="filtro-hasta"
              className="campo-control"
              max={FECHA_MAXIMA}
              value={borrador.hasta}
              onChange={cambiar('hasta')}
            />
          </div>
          <SeleccionVarios rotulo={ROTULOS.estado} opciones={OPCIONES_ESTADO} atajos={ATAJOS_ESTADO}
            elegidos={listaDe(borrador.estado)} alCambiar={elegir('estado')} />
          <SeleccionVarios rotulo={ROTULOS.apartamento} opciones={opcionesApartamento.map((codigo) => [codigo, codigo] as const)}
            elegidos={listaDe(borrador.apartamento)} alCambiar={elegir('apartamento')} />
          <SeleccionVarios rotulo={ROTULOS.canal} opciones={OPCIONES_CANAL}
            elegidos={listaDe(borrador.canal)} alCambiar={elegir('canal')} />
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
                  <td className="derecha codigo importe">{dinero(totalConIva(reserva.valor))}</td>
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
                  <span className="codigo importe">{dinero(totalConIva(reserva.valor))}</span>
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
