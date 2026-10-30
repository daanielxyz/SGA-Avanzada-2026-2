import { useEffect, useState } from 'react'
import {
  buscarDisponibles,
  listarApartamentos,
  type ApartamentoPublico,
  type Estadia,
  type Grupo,
} from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import FormularioBusqueda from '../busqueda/FormularioBusqueda'
import TarjetaApartamento from '../busqueda/TarjetaApartamento'
import { fechaCorta, noches } from '../formato'

/** Lo que se ve debajo del filtro: todos los apartamentos o los libres para unas fechas. */
type Vista =
  | { estado: 'cargando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'todos'; apartamentos: ApartamentoPublico[] }
  | { estado: 'filtrados'; estadia: Estadia; grupo: Grupo; apartamentos: ApartamentoPublico[] }

function mensajeDe(error: unknown) {
  return error instanceof ErrorApi ? error.message : 'Algo salió mal.'
}

/**
 * Página de inicio: todos los apartamentos con lo que paga un adulto su próxima noche libre, y un filtro opcional por
 * fechas y personas que deja solo los libres con el precio del grupo (CU-07 · CU-09 · RN-06).
 *
 * @param umbral edad desde la que se paga, del alojamiento; vacía mientras llega
 */
export default function Inicio({ umbral }: { umbral: number | null }) {
  const [vista, setVista] = useState<Vista>({ estado: 'cargando' })

  async function verTodos() {
    setVista({ estado: 'cargando' })
    try {
      setVista({ estado: 'todos', apartamentos: await listarApartamentos() })
    } catch (error) {
      setVista({ estado: 'error', mensaje: mensajeDe(error) })
    }
  }

  async function filtrar(estadia: Estadia, grupo: Grupo) {
    setVista({ estado: 'cargando' })
    try {
      setVista({ estado: 'filtrados', estadia, grupo, apartamentos: await buscarDisponibles(estadia, grupo) })
    } catch (error) {
      setVista({ estado: 'error', mensaje: mensajeDe(error) })
    }
  }

  useEffect(() => {
    let vigente = true // si la página se cierra antes de que responda, no se toca nada
    listarApartamentos()
      .then((apartamentos) => vigente && setVista({ estado: 'todos', apartamentos }))
      .catch((error: unknown) => vigente && setVista({ estado: 'error', mensaje: mensajeDe(error) }))
    return () => {
      vigente = false
    }
  }, [])

  return (
    <main>
      <h1>Reserva tu estadía</h1>
      <p className="suave">Elige un apartamento o busca tus fechas para ver solo los libres.</p>

      <FormularioBusqueda buscando={vista.estado === 'cargando'} umbral={umbral} alBuscar={filtrar} />

      <section aria-live="polite" className="resultados">
        {vista.estado === 'cargando' && <p className="suave">Cargando apartamentos…</p>}
        {vista.estado === 'error' && (
          <p role="alert" className="aviso">
            {vista.mensaje}
          </p>
        )}
        {vista.estado === 'todos' && <Todos apartamentos={vista.apartamentos} />}
        {vista.estado === 'filtrados' && <Filtrados {...vista} alVerTodos={verTodos} />}
      </section>
    </main>
  )
}

function Todos({ apartamentos }: { apartamentos: ApartamentoPublico[] }) {
  if (apartamentos.length === 0) {
    return <h2>Todavía no hay apartamentos a la venta</h2>
  }
  return (
    <>
      <h2>Nuestros apartamentos</h2>
      <div className="rejilla">
        {apartamentos.map((a) => (
          <TarjetaApartamento key={a.codigo} apartamento={a} />
        ))}
      </div>
    </>
  )
}

type PropsFiltrados = Extract<Vista, { estado: 'filtrados' }> & { alVerTodos: () => void }

function Filtrados({ estadia, grupo, apartamentos, alVerTodos }: PropsFiltrados) {
  const n = noches(estadia.entrada, estadia.salida)
  const personas = grupo.adultos + grupo.ninos
  const resumen = `${fechaCorta(estadia.entrada)} a ${fechaCorta(estadia.salida)} · ${n} ${n === 1 ? 'noche' : 'noches'}`
  const verTodos = (
    <button type="button" className="secundario" onClick={alVerTodos}>
      Ver todos los apartamentos
    </button>
  )

  if (apartamentos.length === 0) {
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
        {apartamentos.length} {apartamentos.length === 1 ? 'apartamento libre' : 'apartamentos libres'}
      </h2>
      <p className="suave">{resumen}</p>
      {verTodos}
      <div className="rejilla">
        {apartamentos.map((a) => (
          <TarjetaApartamento key={a.codigo} apartamento={a} busqueda={{ estadia, grupo }} />
        ))}
      </div>
    </>
  )
}
