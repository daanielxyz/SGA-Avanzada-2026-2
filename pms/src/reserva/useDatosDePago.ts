import { useCallback, useEffect, useState } from 'react'
import { consultarAlojamiento, type Alojamiento } from '../api/alojamiento'
import { consultarFolio, type Folio } from '../api/folio'

export type DatosDePago = { folio: Folio; alojamiento: Alojamiento }
type Resultado = ({ clave: string } & DatosDePago) | { clave: string; error: unknown }

/**
 * El folio y el alojamiento de una reserva, que llegan aparte de ella.
 *
 * @returns `datos` nulo mientras carga o si falló; `recargar` los actualiza sin volver a mostrar «cargando»
 */
export function useDatosDePago(codigo: string) {
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const clave = `${codigo}#${intento}`

  const pedirDatos = useCallback(async (): Promise<DatosDePago> => {
    const [folio, alojamiento] = await Promise.all([consultarFolio(codigo), consultarAlojamiento()])
    return { folio, alojamiento }
  }, [codigo])

  useEffect(() => {
    let vigente = true
    pedirDatos()
      .then((datos) => {
        if (vigente) setResultado({ clave, ...datos })
      })
      .catch((error: unknown) => {
        if (vigente) setResultado({ clave, error })
      })
    return () => {
      vigente = false
    }
  }, [clave, pedirDatos])

  const actual = resultado?.clave === clave ? resultado : null
  return {
    cargando: actual === null,
    datos: actual && !('error' in actual) ? { folio: actual.folio, alojamiento: actual.alojamiento } : null,
    error: actual && 'error' in actual ? actual.error : null,
    reintentar: () => setIntento((valor) => valor + 1),
    recargar: () =>
      pedirDatos()
        .then((datos) => setResultado({ clave, ...datos }))
        .catch(() => undefined),
    ponerFolio: (folio: Folio) =>
      setResultado((previo) => (previo && !('error' in previo) ? { ...previo, folio } : previo)),
  }
}
