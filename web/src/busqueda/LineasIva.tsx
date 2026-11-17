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

/** La aclaración bajo un total con IVA: cuánto lleva y quién no lo paga. */
export function NotaIva({ precio }: { precio: Precio }) {
  if (precio.ivaExento) {
    return <p className="nota-iva suave">Sin IVA: quien llega con pasaporte no lo paga.</p>
  }
  return (
    <p className="nota-iva suave">
      Incluye {enPesos(precio.iva ?? 0)} de IVA ({precio.ivaPorcentaje} %). Quien llega con pasaporte no lo paga.
    </p>
  )
}
