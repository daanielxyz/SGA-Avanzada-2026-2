import { useState, type FormEvent } from 'react'
import type { Estadia, Grupo } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'

interface Props {
  buscando: boolean
  /** Edad desde la que se paga, del alojamiento (RN-06); vacía mientras llega. */
  umbral: number | null
  alBuscar: (estadia: Estadia, grupo: Grupo) => void
}

/** Fechas y quiénes viajan. Revisa lo obvio antes de llamar a la API; las reglas de verdad las revisa el backend. */
export default function FormularioBusqueda({ buscando, umbral, alBuscar }: Props) {
  const hoy = fechaDesdeHoy()
  const [entrada, setEntrada] = useState(fechaDesdeHoy(1))
  const [salida, setSalida] = useState(fechaDesdeHoy(3))
  const [adultos, setAdultos] = useState(2)
  const [ninos, setNinos] = useState(0)
  const [problema, setProblema] = useState<string | null>(null)

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    // Las fechas AAAA-MM-DD se pueden comparar como texto
    if (entrada < hoy) {
      setProblema('La llegada no puede ser antes de hoy.')
    } else if (salida <= entrada) {
      setProblema('La salida debe ser al menos un día después de la llegada.')
    } else {
      setProblema(null)
      alBuscar({ entrada, salida }, { adultos, ninos })
    }
  }

  return (
    <form className="busqueda" onSubmit={enviar} noValidate>
      <label>
        Llegada
        <input type="date" value={entrada} min={hoy} required onChange={(e) => setEntrada(e.target.value)} />
      </label>
      <label>
        Salida
        <input type="date" value={salida} min={entrada} required onChange={(e) => setSalida(e.target.value)} />
      </label>
      <CampoPersonas
        nombre="Adultos"
        ayuda={umbral === null ? 'Pagan' : `${umbral} años o más`}
        minimo={1}
        valor={adultos}
        alCambiar={setAdultos}
      />
      <CampoPersonas
        nombre="Niños"
        ayuda={umbral === null ? 'No pagan' : `Menores de ${umbral}, no pagan`}
        minimo={0}
        valor={ninos}
        alCambiar={setNinos}
      />
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

interface PropsCampo {
  nombre: string
  /** La edad que cuenta, debajo del campo; los lectores de pantalla la leen después del nombre. */
  ayuda: string
  minimo: number
  valor: number
  alCambiar: (valor: number) => void
}

/** Un número de personas, nunca menor que su mínimo. */
function CampoPersonas({ nombre, ayuda, minimo, valor, alCambiar }: PropsCampo) {
  const id = `personas-${nombre}`
  return (
    <div className="campo">
      <label htmlFor={id}>{nombre}</label>
      <input
        id={id}
        type="number"
        inputMode="numeric"
        min={minimo}
        max={20}
        value={valor}
        aria-describedby={`${id}-ayuda`}
        onChange={(e) => alCambiar(Math.max(minimo, Number(e.target.value) || minimo))}
      />
      <span id={`${id}-ayuda`} className="ayuda">
        {ayuda}
      </span>
    </div>
  )
}
