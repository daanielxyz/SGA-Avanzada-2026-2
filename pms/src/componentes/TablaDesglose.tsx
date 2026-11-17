import type { Valor } from '../api/reservas'
import { dinero, fechaCorta } from '../formato'
import { lineaIva, totalConIva, valorAlojamiento } from '../reserva/valor'

/**
 * Valor por noche y total de una estancia. En pantallas angostas la tarifa y los ocupantes pasan debajo de la fecha:
 * el subtotal se queda a la vista para que las noches sumen el alojamiento. Si la estancia lleva IVA, abajo van el
 * alojamiento, el IVA y el total con IVA (DEC-134).
 *
 * @param idTitulo el `id` del título que la nombra
 */
export default function TablaDesglose({ idTitulo, valor }: { idTitulo: string; valor: Valor }) {
  const { desglose } = valor
  const iva = lineaIva(valor)
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
      {iva && (
        <dl className="desglose-iva">
          <div>
            <dt>Alojamiento</dt>
            <dd className="codigo">{dinero(valorAlojamiento(valor))}</dd>
          </div>
          <div>
            <dt>{iva.texto}</dt>
            <dd className="codigo">{dinero(iva.valor)}</dd>
          </div>
        </dl>
      )}
      <p className="desglose-total">
        <span className="desglose-total-rotulo">Total</span>
        <span className="codigo desglose-total-valor">{dinero(totalConIva(valor))}</span>
      </p>
    </div>
  )
}
