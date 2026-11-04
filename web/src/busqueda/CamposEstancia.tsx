import type { MouseEvent } from 'react'
import type { Estadia, Grupo } from '../api/apartamentos'
import { fechaDesdeHoy, masDias } from '../formato'

interface Props {
  estadia: Estadia
  grupo: Grupo
  /** Edad desde la que se paga, del alojamiento (RN-06); vacía mientras llega. */
  umbral: number | null
  /** En la página de un apartamento, su capacidad: tope de cada campo de personas. En la búsqueda no hay tope. */
  capacidad?: number
  /** La tecla Enter del teclado del celular: «buscar» en el filtro, «listo» en la página del apartamento. */
  teclaEnter?: 'search' | 'done'
  alCambiar: (estadia: Estadia, grupo: Grupo) => void
}

/** Llegada, salida, adultos y niños: los mismos campos en la búsqueda y en la página de cada apartamento. */
export default function CamposEstancia({ estadia, grupo, umbral, capacidad, teclaEnter = 'done', alCambiar }: Props) {
  const hoy = fechaDesdeHoy()

  // Si la llegada alcanza la salida, la salida se corre para conservar al menos una noche
  function cambiarEntrada(entrada: string) {
    alCambiar({ entrada, salida: estadia.salida > entrada ? estadia.salida : masDias(entrada, 1) }, grupo)
  }

  return (
    <>
      <label>
        Llegada
        <input type="date" value={estadia.entrada} min={hoy} required onClick={abrirCalendario}
          onChange={(e) => cambiarEntrada(e.target.value)} />
      </label>
      <label>
        Salida
        <input type="date" value={estadia.salida} min={masDias(estadia.entrada, 1)} required onClick={abrirCalendario}
          onChange={(e) => alCambiar({ ...estadia, salida: e.target.value }, grupo)} />
      </label>
      <CampoPersonas
        nombre="Adultos"
        ayuda={umbral === null ? null : `${umbral} años o más`}
        minimo={1}
        maximo={capacidad}
        teclaEnter={teclaEnter}
        valor={grupo.adultos}
        alCambiar={(adultos) => alCambiar(estadia, { ...grupo, adultos })}
      />
      <CampoPersonas
        nombre="Niños"
        ayuda={umbral === null ? null : `Menores de ${umbral}`}
        minimo={0}
        maximo={capacidad}
        teclaEnter={teclaEnter}
        valor={grupo.ninos}
        alCambiar={(ninos) => alCambiar(estadia, { ...grupo, ninos })}
      />
    </>
  )
}

// Todo el campo abre el calendario, no solo su ícono. Sin showPicker en el navegador, queda el comportamiento normal
function abrirCalendario(evento: MouseEvent<HTMLInputElement>) {
  try {
    evento.currentTarget.showPicker?.()
  } catch {
    // El navegador puede negarlo (por ejemplo, dentro de un iframe); el ícono sigue funcionando
  }
}

interface PropsCampo {
  nombre: string
  /** La edad que cuenta, debajo del campo; se lee después del nombre en lectores de pantalla. Vacía mientras llega. */
  ayuda: string | null
  minimo: number
  maximo?: number
  teclaEnter: 'search' | 'done'
  valor: number
  alCambiar: (valor: number) => void
}

/** Un número de personas, nunca menor que su mínimo. */
function CampoPersonas({ nombre, ayuda, minimo, maximo, teclaEnter, valor, alCambiar }: PropsCampo) {
  const id = `personas-${nombre}`
  return (
    <div className="campo">
      <label htmlFor={id}>{nombre}</label>
      <input
        id={id}
        type="number"
        inputMode="numeric"
        enterKeyHint={teclaEnter}
        min={minimo}
        max={maximo}
        value={valor}
        aria-describedby={ayuda ? `${id}-ayuda` : undefined}
        onChange={(e) => alCambiar(Math.max(minimo, Number(e.target.value) || minimo))}
      />
      {ayuda && (
        <span id={`${id}-ayuda`} className="ayuda">
          {ayuda}
        </span>
      )}
    </div>
  )
}
