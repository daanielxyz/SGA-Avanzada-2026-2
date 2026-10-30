import type { Estadia, Grupo } from '../api/apartamentos'
import { fechaDesdeHoy, masDias } from '../formato'

interface Props {
  estadia: Estadia
  grupo: Grupo
  /** Edad desde la que se paga, del alojamiento (RN-06); vacía mientras llega. */
  umbral: number | null
  /** En la página de un apartamento, su capacidad: tope de cada campo de personas. En la búsqueda no hay tope. */
  capacidad?: number
  alCambiar: (estadia: Estadia, grupo: Grupo) => void
}

/** Llegada, salida, adultos y niños: los mismos campos en la búsqueda y en la página de cada apartamento. */
export default function CamposEstancia({ estadia, grupo, umbral, capacidad, alCambiar }: Props) {
  const hoy = fechaDesdeHoy()

  // Si la llegada alcanza la salida, la salida se corre para conservar al menos una noche
  function cambiarEntrada(entrada: string) {
    alCambiar({ entrada, salida: estadia.salida > entrada ? estadia.salida : masDias(entrada, 1) }, grupo)
  }

  return (
    <>
      <label>
        Llegada
        <input type="date" value={estadia.entrada} min={hoy} required onChange={(e) => cambiarEntrada(e.target.value)} />
      </label>
      <label>
        Salida
        <input type="date" value={estadia.salida} min={masDias(estadia.entrada, 1)} required
          onChange={(e) => alCambiar({ ...estadia, salida: e.target.value }, grupo)} />
      </label>
      <CampoPersonas
        nombre="Adultos"
        ayuda={umbral === null ? 'Pagan' : `${umbral} años o más`}
        minimo={1}
        maximo={capacidad}
        valor={grupo.adultos}
        alCambiar={(adultos) => alCambiar(estadia, { ...grupo, adultos })}
      />
      <CampoPersonas
        nombre="Niños"
        ayuda={umbral === null ? 'No pagan' : `Menores de ${umbral}, no pagan`}
        minimo={0}
        maximo={capacidad}
        valor={grupo.ninos}
        alCambiar={(ninos) => alCambiar(estadia, { ...grupo, ninos })}
      />
    </>
  )
}

interface PropsCampo {
  nombre: string
  /** La edad que cuenta, debajo del campo; los lectores de pantalla la leen después del nombre. */
  ayuda: string
  minimo: number
  maximo?: number
  valor: number
  alCambiar: (valor: number) => void
}

/** Un número de personas, nunca menor que su mínimo. */
function CampoPersonas({ nombre, ayuda, minimo, maximo, valor, alCambiar }: PropsCampo) {
  const id = `personas-${nombre}`
  return (
    <div className="campo">
      <label htmlFor={id}>{nombre}</label>
      <input
        id={id}
        type="number"
        inputMode="numeric"
        min={minimo}
        max={maximo}
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
