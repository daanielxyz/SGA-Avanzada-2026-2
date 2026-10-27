import { useState, type FormEvent } from 'react'
import type { Estadia } from '../api/apartamentos'
import { fechaDesdeHoy } from '../formato'

interface Props {
  buscando: boolean
  alBuscar: (estadia: Estadia, ocupantes: number) => void
}

/** Fechas y tamaño del grupo. Revisa lo obvio antes de llamar a la API; las reglas de verdad las revisa el backend. */
export default function FormularioBusqueda({ buscando, alBuscar }: Props) {
  const hoy = fechaDesdeHoy()
  const [entrada, setEntrada] = useState(fechaDesdeHoy(1))
  const [salida, setSalida] = useState(fechaDesdeHoy(3))
  const [ocupantes, setOcupantes] = useState(2)
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
      alBuscar({ entrada, salida }, ocupantes)
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
      <label>
        Personas
        <input
          type="number"
          inputMode="numeric"
          min={1}
          max={20}
          value={ocupantes}
          onChange={(e) => setOcupantes(Math.max(1, Number(e.target.value) || 1))}
        />
      </label>
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
