import { useState, type FormEvent } from 'react'
import { cotizar, type Cotizacion, type Estadia } from '../api/apartamentos'
import { ErrorApi } from '../api/cliente'
import { enPesos, fechaCorta, fechaDesdeHoy } from '../formato'

interface Props {
  apartamento: string
  estadia: Estadia
  ocupantes: number
}

/**
 * Pide la fecha de nacimiento de cada persona y muestra el precio noche por noche. La edad a la fecha de llegada
 * decide quién paga (RN-06). TODO(equipo): medir en el cierre 1 si pedir las fechas aquí le estorba al visitante.
 */
export default function Cotizador({ apartamento, estadia, ocupantes }: Props) {
  const hoy = fechaDesdeHoy()
  const [fechas, setFechas] = useState<string[]>(() => Array(ocupantes).fill(''))
  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null)
  const [calculando, setCalculando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)

  function cambiarFecha(posicion: number, valor: string) {
    setFechas((actuales) => actuales.map((f, i) => (i === posicion ? valor : f)))
    setCotizacion(null)
  }

  async function calcular(evento: FormEvent) {
    evento.preventDefault()
    if (fechas.some((f) => f === '' || f > hoy)) {
      setProblema('Escribe una fecha de nacimiento válida para cada persona.')
      return
    }
    setProblema(null)
    setCalculando(true)
    try {
      setCotizacion(await cotizar(apartamento, estadia, fechas))
    } catch (error) {
      setProblema(error instanceof ErrorApi ? error.message : 'No pudimos calcular el precio.')
    } finally {
      setCalculando(false)
    }
  }

  return (
    <div className="cotizador">
      <form onSubmit={calcular} noValidate>
        <p className="suave">El precio depende de la edad de cada persona el día de llegada.</p>
        {fechas.map((fecha, i) => (
          <label key={i}>
            Fecha de nacimiento, persona {i + 1}
            <input type="date" value={fecha} max={hoy} required onChange={(e) => cambiarFecha(i, e.target.value)} />
          </label>
        ))}
        <button type="submit" disabled={calculando}>
          {calculando ? 'Calculando…' : 'Calcular precio'}
        </button>
        {problema && (
          <p role="alert" className="aviso">
            {problema}
          </p>
        )}
      </form>

      {cotizacion && (
        <table>
          <caption>Precio de la estadía</caption>
          <thead>
            <tr>
              <th scope="col">Noche</th>
              <th scope="col">Por persona</th>
              <th scope="col">Pagan</th>
              <th scope="col">Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {cotizacion.desglose.map((n) => (
              <tr key={n.noche}>
                <td>{fechaCorta(n.noche)}</td>
                <td>{enPesos(n.tarifa)}</td>
                <td>{n.ocupantesFacturables}</td>
                <td>{enPesos(n.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row" colSpan={3}>
                Total
              </th>
              <td>{enPesos(cotizacion.total)}</td>
            </tr>
          </tfoot>
        </table>
      )}
    </div>
  )
}
