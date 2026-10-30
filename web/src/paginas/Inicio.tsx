import { useState } from 'react'
import { buscarDisponibles, type ApartamentoPublico, type Estadia } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import FormularioBusqueda from '../busqueda/FormularioBusqueda'
import TarjetaApartamento from '../busqueda/TarjetaApartamento'
import { fechaCorta, noches } from '../formato'

/** Lo que se buscó y lo que respondió la API. */
type Busqueda =
  | { estado: 'sin-buscar' }
  | { estado: 'buscando' }
  | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; estadia: Estadia; ocupantes: number; apartamentos: ApartamentoPublico[] }

/** Página de inicio: buscar apartamentos libres por fechas y personas, y ver el precio de cada uno (CU-07 · CU-09). */
export default function Inicio() {
  const [busqueda, setBusqueda] = useState<Busqueda>({ estado: 'sin-buscar' })

  async function buscar(estadia: Estadia, ocupantes: number) {
    setBusqueda({ estado: 'buscando' })
    try {
      const apartamentos = await buscarDisponibles(estadia, ocupantes)
      setBusqueda({ estado: 'lista', estadia, ocupantes, apartamentos })
    } catch (error) {
      setBusqueda({ estado: 'error', mensaje: error instanceof ErrorApi ? error.message : 'Algo salió mal.' })
    }
  }

  return (
    <main>
      <h1>Reserva tu estadía</h1>
      <p className="suave">Busca tus fechas y mira el precio de cada apartamento antes de reservar.</p>

      <FormularioBusqueda buscando={busqueda.estado === 'buscando'} alBuscar={buscar} />

      <section aria-live="polite" className="resultados">
        {busqueda.estado === 'error' && (
          <p role="alert" className="aviso">
            {busqueda.mensaje}
          </p>
        )}
        {busqueda.estado === 'lista' && <Resultados {...busqueda} />}
      </section>
    </main>
  )
}

function Resultados({ estadia, ocupantes, apartamentos }: Extract<Busqueda, { estado: 'lista' }>) {
  const n = noches(estadia.entrada, estadia.salida)
  const resumen = `${fechaCorta(estadia.entrada)} a ${fechaCorta(estadia.salida)} · ${n} ${n === 1 ? 'noche' : 'noches'}`

  if (apartamentos.length === 0) {
    return (
      <>
        <h2>No hay apartamentos libres</h2>
        <p>
          Para {ocupantes} {ocupantes === 1 ? 'persona' : 'personas'}, {resumen}. Prueba con otras fechas o menos
          personas.
        </p>
      </>
    )
  }
  return (
    <>
      <h2>
        {apartamentos.length} {apartamentos.length === 1 ? 'apartamento libre' : 'apartamentos libres'}
      </h2>
      <p className="suave">{resumen}</p>
      {apartamentos.map((a) => (
        // La clave cambia con cada búsqueda para que el precio calculado de otras fechas no se quede en pantalla
        <TarjetaApartamento
          key={`${a.codigo}-${estadia.entrada}-${estadia.salida}-${ocupantes}`}
          apartamento={a}
          estadia={estadia}
          ocupantes={ocupantes}
        />
      ))}
    </>
  )
}
