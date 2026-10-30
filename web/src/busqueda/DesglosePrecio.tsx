import type { Precio } from '../api/apartamentos'
import { enPesos, fechaCorta } from '../formato'

/** El precio noche por noche, con la temporada de cada una ya aplicada, y el total. */
export default function DesglosePrecio({ precio }: { precio: Precio }) {
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
        <tr>
          <th scope="row" colSpan={3}>
            Total
          </th>
          <td>{enPesos(precio.total)}</td>
        </tr>
      </tfoot>
    </table>
  )
}
