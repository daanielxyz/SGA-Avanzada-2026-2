import { useEffect, useRef, useState } from 'react'
import type { Alojamiento } from '../api/alojamiento'
import {
  buscarDisponibles,
  listarApartamentos,
  type ApartamentoPublico,
  type Estadia,
  type Grupo,
  type Pagina,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import FormularioBusqueda from '../busqueda/FormularioBusqueda'
import Paginacion from '../busqueda/Paginacion'
import TarjetaApartamento from '../busqueda/TarjetaApartamento'
import { fechaCorta, noches } from '../formato'

/** Lo que se ve debajo del filtro: todos los apartamentos o los libres para unas fechas, de 10 en 10. */
type Vista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'todos'; pagina: Pagina<ApartamentoPublico> }
  | { estado: 'filtrados'; estadia: Estadia; grupo: Grupo; pagina: Pagina<ApartamentoPublico> }

function mensajeDe(error: unknown) {
  return error instanceof ErrorApi ? error.message : 'Algo salió mal.'
}

/**
 * Página de inicio: abre con el hotel y debajo todos los apartamentos con lo que paga un adulto su próxima noche libre, y
 * un filtro opcional por fechas y personas que deja solo los libres con el precio del grupo (CU-07 · CU-09 · RN-06).
 * Ambos van de 10 en 10.
 *
 * @param alojamiento nombre, descripción, marca y edad desde la que se paga; vacío mientras llega o si la API no responde
 */
export default function Inicio({ alojamiento }: { alojamiento: Alojamiento | null }) {
  const [vista, setVista] = useState<Vista>({ estado: 'cargando' })
  const [fotoDeRespaldo, setFotoDeRespaldo] = useState<string | null>(null)
  const resultados = useRef<HTMLElement>(null)

  async function verTodos(numero = 0) {
    setVista({ estado: 'cargando' })
    try {
      setVista({ estado: 'todos', pagina: await listarApartamentos(numero) })
    } catch (error) {
      setVista({ estado: 'error', mensaje: mensajeDe(error) })
    }
  }

  async function filtrar(estadia: Estadia, grupo: Grupo, numero = 0) {
    setVista({ estado: 'cargando' })
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
    listarApartamentos()
      .then((pagina) => {
        if (vigente) {
          setVista({ estado: 'todos', pagina })
          setFotoDeRespaldo(primeraFoto(pagina.contenido))
        }
      })
      .catch((error: unknown) => vigente && setVista({ estado: 'error', mensaje: mensajeDe(error) }))
    return () => {
      vigente = false
    }
  }, [])

  return (
    <>
      <Portada alojamiento={alojamiento} foto={alojamiento?.marca?.portadaUrl ?? fotoDeRespaldo} />
      <main className="inicio">
        <FormularioBusqueda buscando={vista.estado === 'cargando'} filtrando={vista.estado === 'filtrados'}
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
    </>
  )
}

// Sin foto de portada propia, un hotel igual abre con una imagen: la de su primer apartamento que tenga
function primeraFoto(apartamentos: ApartamentoPublico[]) {
  const fotos = apartamentos.flatMap((a) => a.imagenes)
  return (fotos.find((f) => f.principal) ?? fotos[0])?.url ?? null
}

// Valor inicial de sga.alojamiento.* mientras el hotel no escribe el suyo: no se le muestra al huésped
function escrito(dato: string | null | undefined) {
  return dato && dato !== 'Por definir' ? dato : null
}

function Portada({ alojamiento, foto }: { alojamiento: Alojamiento | null; foto: string | null }) {
  const ciudad = escrito(alojamiento?.ciudad)
  return (
    <section className="portada">
      {foto && <img src={foto} alt="" />}
      <div className="portada-texto">
        <h1>{alojamiento?.nombre ?? 'Reserva tu estadía'}</h1>
        {ciudad && <p className="portada-ciudad">{ciudad}</p>}
        <p>{escrito(alojamiento?.descripcion) ?? 'Elige un apartamento o busca tus fechas para ver solo los libres.'}</p>
      </div>
    </section>
  )
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
