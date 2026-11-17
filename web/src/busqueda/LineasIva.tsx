import type { Precio } from '../api/apartamentos'
import { enPesos } from '../formato'

/**
 * Dentro de una lista de valores (`<dl>`), cuánto es alojamiento y cuánto IVA (DEC-134 · DEC-141). El alojamiento ya
 * va sin el IVA que traen las tarifas, así que las dos líneas suman lo que se paga por las noches.
 */
export default function LineasIva({ precio, alojamiento = 'Alojamiento' }: { precio: Precio; alojamiento?: string }) {
  return (
    <>
      <dt>{alojamiento}</dt>
      <dd>{enPesos(precio.alojamiento ?? precio.total)}</dd>
      <dt>IVA ({precio.ivaPorcentaje} %)</dt>
      <dd>{precio.ivaExento ? 'Exento' : enPesos(precio.iva ?? 0)}</dd>
    </>
  )
}

/** Bajo un total con IVA, quién no lo paga: el cuánto ya está en las líneas de arriba. */
export function NotaIva({ precio }: { precio: Precio }) {
  return (
    <p className="nota-iva suave">
      {precio.ivaExento ? 'Sin IVA: quien llega con pasaporte no lo paga.' : 'Quien llega con pasaporte no paga IVA.'}
    </p>
  )
}
