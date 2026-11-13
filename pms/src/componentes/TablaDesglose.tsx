import { dinero, fechaCorta } from '../formato'

interface Noche {
  noche: string
  temporada: string
  /** DEC-117: el nombre que recepción reconoce; si no llega, el id. */
  temporadaNombre?: string
  tarifa: number
  ocupantesFacturables: number
  subtotal: number
}

/**
 * Valor por noche y total de una estancia. En pantallas angostas la tarifa y los ocupantes pasan debajo de la fecha:
 * el subtotal se queda a la vista para que las noches sumen el total.
 *
 * @param idTitulo el `id` del título que la nombra
 */
export default function TablaDesglose({ idTitulo, desglose, total }: {
  idTitulo: string
  desglose: Noche[]
  total: number
}) {
  return (
    <div className="tarjeta tabla-marco">
      <div className="desplazable" tabIndex={0} role="group" aria-labelledby={idTitulo}>
        <table className="tabla tabla-desglose" aria-labelledby={idTitulo}>
          <thead>
            <tr>
              <th scope="col">Noche</th>
              <th scope="col">Temporada</th>
              <th scope="col" className="derecha">
                Tarifa por ocupante facturable
              </th>
              <th scope="col" className="derecha">
                Ocupantes facturables
              </th>
              <th scope="col" className="derecha">
                Subtotal
              </th>
            </tr>
          </thead>
          <tbody>
            {desglose.map((noche) => (
              <tr key={noche.noche}>
                <td>
                  {fechaCorta(noche.noche)}
                  <span className="desglose-detalle" aria-hidden="true">
                    <span className="codigo">{dinero(noche.tarifa)}</span> × {noche.ocupantesFacturables}
                  </span>
                </td>
                <td>{noche.temporadaNombre || noche.temporada}</td>
                <td className="derecha codigo">{dinero(noche.tarifa)}</td>
                <td className="derecha numero">{noche.ocupantesFacturables}</td>
                <td className="derecha codigo">{dinero(noche.subtotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="desglose-total">
        <span className="desglose-total-rotulo">Total</span>
        <span className="codigo desglose-total-valor">{dinero(total)}</span>
      </p>
    </div>
  )
}
