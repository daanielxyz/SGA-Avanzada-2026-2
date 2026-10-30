import { useState, type FormEvent } from 'react'
import type { Estadia, Grupo } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'
import CamposEstancia from './CamposEstancia'
import { problemaDeEstadia } from './estadia'

interface Props {
  buscando: boolean
  /** Edad desde la que se paga, del alojamiento (RN-06); vacía mientras llega. */
  umbral: number | null
  alBuscar: (estadia: Estadia, grupo: Grupo) => void
}

/** El filtro del inicio: deja solo los apartamentos libres para unas fechas y un grupo. */
export default function FormularioBusqueda({ buscando, umbral, alBuscar }: Props) {
  const [estadia, setEstadia] = useState<Estadia>({ entrada: fechaDesdeHoy(1), salida: fechaDesdeHoy(3) })
  const [grupo, setGrupo] = useState<Grupo>({ adultos: 2, ninos: 0 })
  const [problema, setProblema] = useState<string | null>(null)

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrado = problemaDeEstadia(estadia)
    setProblema(encontrado)
    if (encontrado === null) {
      alBuscar(estadia, grupo)
    }
  }

  return (
    <form className="busqueda" onSubmit={enviar} noValidate>
      <CamposEstancia estadia={estadia} grupo={grupo} umbral={umbral} teclaEnter="search" alCambiar={(e, g) => {
        setEstadia(e)
        setGrupo(g)
      }} />
      <button type="submit" disabled={buscando}>
        {buscando ? 'Buscando…' : 'Buscar'}
      </button>
      {problema && (
        <p role="alert" className="aviso">
          {problema}
        </p>
      )}
    </form>
  )
}
