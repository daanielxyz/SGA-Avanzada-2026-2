import { useState, type FormEvent } from 'react'
import type { Estadia, Grupo } from '../api/apartamentos'
import { fechaCorta, fechaDesdeHoy } from '../formato'
import CamposEstancia from './CamposEstancia'
import { problemaDeEstadia } from './estadia'

interface Props {
  buscando: boolean
  /** Si los resultados de abajo son los de este filtro: el resumen del celular muestra entonces sus fechas. */
  filtrando: boolean
  /** Edad desde la que se paga, del alojamiento (RN-06); vacía mientras llega. */
  umbral: number | null
  alBuscar: (estadia: Estadia, grupo: Grupo) => void
}

/**
 * El filtro del inicio: deja solo los apartamentos libres para unas fechas y un grupo. En el celular es una barra con el
 * resumen que se despliega al tocarla, para que los campos no tapen los apartamentos; en pantallas anchas, una fila.
 */
export default function FormularioBusqueda({ buscando, filtrando, umbral, alBuscar }: Props) {
  const [estadia, setEstadia] = useState<Estadia>({ entrada: fechaDesdeHoy(1), salida: fechaDesdeHoy(3) })
  const [grupo, setGrupo] = useState<Grupo>({ adultos: 2, ninos: 0 })
  const [problema, setProblema] = useState<string | null>(null)
  const [abierta, setAbierta] = useState(false)

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const encontrado = problemaDeEstadia(estadia)
    setProblema(encontrado)
    if (encontrado === null) {
      setAbierta(false)
      alBuscar(estadia, grupo)
    }
  }

  const personas = grupo.adultos + grupo.ninos
  return (
    <form className="busqueda" data-abierta={abierta} onSubmit={enviar} noValidate>
      <button type="button" className="busqueda-resumen" aria-expanded={abierta} aria-controls="busqueda-campos"
        onClick={() => setAbierta((a) => !a)}>
        <span>
          {filtrando && estadia.entrada && estadia.salida
            ? `${fechaCorta(estadia.entrada)} a ${fechaCorta(estadia.salida)}`
            : '¿Cuándo vienes?'}
        </span>
        <span className="suave">
          {personas} {personas === 1 ? 'persona' : 'personas'}
        </span>
      </button>
      <div className="busqueda-campos" id="busqueda-campos">
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
      </div>
    </form>
  )
}
