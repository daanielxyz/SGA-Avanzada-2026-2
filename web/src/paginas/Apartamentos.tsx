import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import type { Alojamiento } from '../api/alojamiento'
import {
  buscarDisponibles,
  listarApartamentos,
  type ApartamentoPublico,
  type Estadia,
  type Grupo,
  type Pagina,
} from '../api/apartamentos'
import { explicar } from '../api/mensajes'
import { problemaDeEstadia } from '../busqueda/estadia'
import FormularioBusqueda from '../busqueda/FormularioBusqueda'
import Paginacion from '../busqueda/Paginacion'
import TarjetaApartamento from '../busqueda/TarjetaApartamento'
import { fechaCorta, noches } from '../formato'

/** Lo que se ve debajo del filtro: todos los apartamentos o los libres para unas fechas, de 10 en 10. */
type Vista =
  | { estado: 'cargando'; porBusqueda: boolean }
  | { estado: 'error'; mensaje: string }
  | { estado: 'todos'; pagina: Pagina<ApartamentoPublico> }
  | { estado: 'filtrados'; estadia: Estadia; grupo: Grupo; pagina: Pagina<ApartamentoPublico> }

function mensajeDe(error: unknown) {
  return explicar(error, 'No pudimos cargar los apartamentos. Intenta de nuevo en un momento.')
}

/**
 * Los apartamentos: todos con lo que paga un adulto su próxima noche libre, y un filtro opcional por fechas y personas
 * que deja solo los libres con el precio del grupo (CU-07 · CU-09 · RN-06). Ambos van de 10 en 10. Si llega desde la
 * presentación con fechas en la dirección (`?entrada=…&salida=…&adultos=…&ninos=…`), abre ya filtrado.
 *
 * @param alojamiento edad desde la que se paga; vacío mientras llega o si la API falla
 */
export default function Apartamentos({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const [parametros] = useSearchParams()
  const [pedida] = useState(() => busquedaDe(parametros))
  const [vista, setVista] = useState<Vista>({ estado: 'cargando', porBusqueda: pedida !== null })
  const resultados = useRef<HTMLElement>(null)

  async function verTodos(numero = 0) {
    setVista({ estado: 'cargando', porBusqueda: false })
    try {
      setVista({ estado: 'todos', pagina: await listarApartamentos(numero) })
    } catch (error) {
      setVista({ estado: 'error', mensaje: mensajeDe(error) })
    }
  }

  async function filtrar(estadia: Estadia, grupo: Grupo, numero = 0) {
    setVista({ estado: 'cargando', porBusqueda: true })
    try {
      setVista({ estado: 'filtrados', estadia, grupo, pagina: await buscarDisponibles(estadia, grupo, numero) })
    } catch (error) {
      setVista({ estado: 'error', mensaje: mensajeDe(error) })
    }
  }

  // Al pasar de página, el visitante vuelve al comienzo de los resultados, no se queda al final de la anterior
  function irA(numero: number) {
    resultados.current?.scrollIntoView?.({ block: 'start' })
    if (vista.estado === 'filtrados') {
      void filtrar(vista.estadia, vista.grupo, numero)
    } else {
      void verTodos(numero)
    }
  }

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    const consulta = pedida
      ? buscarDisponibles(pedida.estadia, pedida.grupo).then((pagina): Vista => ({ estado: 'filtrados', ...pedida, pagina }))
      : listarApartamentos().then((pagina): Vista => ({ estado: 'todos', pagina }))
    consulta
      .then((v) => vigente && setVista(v))
      .catch((error: unknown) => vigente && setVista({ estado: 'error', mensaje: mensajeDe(error) }))
    return () => {
      vigente = false
    }
  }, [pedida])

  return (
    <main className="apartamentos">
      <header className="cabeza-pagina">
        <h1>Apartamentos</h1>
        <p className="suave">Elige tus fechas para ver solo los libres, con el precio de tu grupo.</p>
      </header>
      <FormularioBusqueda buscando={vista.estado === 'cargando' && vista.porBusqueda}
        filtrando={vista.estado === 'filtrados'} inicial={pedida ?? undefined}
        umbral={alojamiento?.parametros.umbralEdadFacturable ?? null} alBuscar={filtrar} />

      <section aria-live="polite" className="resultados" ref={resultados}>
        {vista.estado === 'cargando' && <Cargando />}
        {vista.estado === 'error' && (
          <p role="alert" className="aviso">
            {vista.mensaje}
          </p>
        )}
        {vista.estado === 'todos' && <Todos pagina={vista.pagina} alCambiarPagina={irA} />}
        {vista.estado === 'filtrados' && <Filtrados {...vista} alVerTodos={() => verTodos()} alCambiarPagina={irA} />}
      </section>
    </main>
  )
}

// Las fechas que trae la dirección; si faltan o no sirven, se ven todos y el filtro queda con sus valores de siempre
function busquedaDe(parametros: URLSearchParams): { estadia: Estadia; grupo: Grupo } | null {
  const estadia = { entrada: parametros.get('entrada') ?? '', salida: parametros.get('salida') ?? '' }
  const adultos = Number(parametros.get('adultos') ?? 2)
  const ninos = Number(parametros.get('ninos') ?? 0)
  const grupoValido = Number.isInteger(adultos) && adultos >= 1 && Number.isInteger(ninos) && ninos >= 0
  if (problemaDeEstadia(estadia) !== null || !grupoValido) {
    return null
  }
  return { estadia, grupo: { adultos, ninos } }
}

// Recuadros con la forma de las filas: los resultados aparecen en su lugar, sin que la página salte
function Cargando() {
  return (
    <>
      <p className="visualmente-oculto">Cargando apartamentos…</p>
      <div className="filas" aria-hidden="true">
        {[0, 1, 2].map((i) => (
          <div key={i} className="tarjeta esqueleto">
            <div className="sin-foto" />
            <div className="esqueleto-lineas">
              <span />
              <span />
              <span />
            </div>
          </div>
        ))}
      </div>
    </>
  )
}

interface PropsTodos {
  pagina: Pagina<ApartamentoPublico>
  alCambiarPagina: (numero: number) => void
}

function Todos({ pagina, alCambiarPagina }: PropsTodos) {
  if (pagina.totalElementos === 0) {
    return <h2>Todavía no hay apartamentos a la venta</h2>
  }
  return (
    <>
      <h2>Nuestros apartamentos</h2>
      <div className="filas">
        {pagina.contenido.map((a) => (
          <TarjetaApartamento key={a.codigo} apartamento={a} />
        ))}
      </div>
      <Paginacion pagina={pagina} alCambiar={alCambiarPagina} />
    </>
  )
}

type PropsFiltrados = Extract<Vista, { estado: 'filtrados' }> & {
  alVerTodos: () => void
  alCambiarPagina: (numero: number) => void
}

function Filtrados({ estadia, grupo, pagina, alVerTodos, alCambiarPagina }: PropsFiltrados) {
  const n = noches(estadia.entrada, estadia.salida)
  const personas = grupo.adultos + grupo.ninos
  const total = pagina.totalElementos
  const resumen = `${fechaCorta(estadia.entrada)} a ${fechaCorta(estadia.salida)} · ${n} ${n === 1 ? 'noche' : 'noches'}`
  const verTodos = (
    <button type="button" className="secundario" onClick={alVerTodos}>
      Ver todos los apartamentos
    </button>
  )

  if (total === 0) {
    return (
      <>
        <h2>No hay apartamentos libres</h2>
        <p>
          Para {personas} {personas === 1 ? 'persona' : 'personas'}, {resumen}. Prueba con otras fechas o menos
          personas.
        </p>
        {verTodos}
      </>
    )
  }
  return (
    <>
      <h2>
        {total} {total === 1 ? 'apartamento libre' : 'apartamentos libres'}
      </h2>
      <div className="resumen-filtro">
        <p className="suave">{resumen}</p>
        {verTodos}
      </div>
      <div className="filas">
        {pagina.contenido.map((a) => (
          <TarjetaApartamento key={a.codigo} apartamento={a} busqueda={{ estadia, grupo }} />
        ))}
      </div>
      <Paginacion pagina={pagina} alCambiar={alCambiarPagina} />
    </>
  )
}
