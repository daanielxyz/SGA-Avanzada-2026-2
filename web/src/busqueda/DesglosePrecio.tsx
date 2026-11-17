import type { Precio } from '../api/apartamentos'
import { enPesos, fechaCorta } from '../formato'
import { llevaIva, valorEstancia } from './iva'

/**
 * El precio noche por noche, con la temporada de cada una ya aplicada, y el total. Si el IVA se suma, va en su
 * línea antes del total; si ya viene en las tarifas, se dice cuánto incluye (DEC-141).
 */
export default function DesglosePrecio({ precio }: { precio: Precio }) {
  const iva = llevaIva(precio) && !precio.ivaExento
  const etiquetaIva = `IVA (${precio.ivaPorcentaje} %)`
  return (
    <table className="desglose">
      <caption>Precio noche por noche</caption>
      <thead>
        <tr>
          <th scope="col">Noche</th>
          <th scope="col">Por adulto</th>
          <th scope="col">Pagan</th>
          <th scope="col">Subtotal</th>
        </tr>
      </thead>
      <tbody>
        {precio.desglose.map((noche) => (
          <tr key={noche.noche}>
            <td>{fechaCorta(noche.noche)}</td>
            <td>{enPesos(noche.tarifa)}</td>
            <td>{noche.ocupantesFacturables}</td>
            <td>{enPesos(noche.subtotal)}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        {iva && !precio.ivaIncluido && (
          <tr>
            <th scope="row" colSpan={3}>{etiquetaIva}</th>
            <td>{enPesos(precio.iva ?? 0)}</td>
          </tr>
        )}
        <tr>
          <th scope="row" colSpan={3}>
            Total
          </th>
          <td>{enPesos(valorEstancia(precio))}</td>
        </tr>
        {iva && precio.ivaIncluido && (
          <tr className="suave">
            <th scope="row" colSpan={3}>Incluye {etiquetaIva}</th>
            <td>{enPesos(precio.iva ?? 0)}</td>
          </tr>
        )}
      </tfoot>
    </table>
  )
}
