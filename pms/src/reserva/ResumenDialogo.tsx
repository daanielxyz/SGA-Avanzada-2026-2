import type { Reserva } from '../api/reservas'
import { cantidad, dinero, fechaCorta, noches } from '../formato'

/** Qué reserva se está tocando, para no equivocarse antes de actuar. */
export default function ResumenDialogo({ reserva }: { reserva: Reserva }) {
  return (
    <dl className="resumen-dialogo">
      <dt>Código</dt>
      <dd className="codigo">{reserva.codigo}</dd>
      <dt>Titular</dt>
      <dd>{reserva.titularNombre ?? reserva.titularId}</dd>
      <dt>Fechas</dt>
      <dd>
        {fechaCorta(reserva.entrada)} → {fechaCorta(reserva.salida)} ·{' '}
        {cantidad(noches(reserva.entrada, reserva.salida), 'noche', 'noches')}
      </dd>
      <dt>Total</dt>
      <dd className="codigo">{dinero(reserva.valor.total)}</dd>
    </dl>
  )
}
