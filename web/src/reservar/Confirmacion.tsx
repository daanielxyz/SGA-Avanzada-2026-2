import { Link } from 'react-router'
import type { PagoAprobado } from '../api/reservas'

interface Props {
  pago: PagoAprobado
  /** Aviso del backend sobre la cuenta del titular, si lo hay (DEC-68). */
  avisoCuenta: string | null
}

/** El final del flujo: la reserva confirmada, con lo que tiene que guardar. */
export default function Confirmacion({ pago, avisoCuenta }: Props) {
  return (
    <div className="confirmacion">
      <p>
        Tu código de reserva es <strong className="codigo-reserva">{pago.codigo}</strong>. Guárdalo: te lo enviamos
        también por correo, con los detalles de tu estadía.
      </p>
      {avisoCuenta && <p className="suave">{avisoCuenta}</p>}
      <div className="confirmacion-acciones">
        <Link to="/mis-reservas" className="boton">Ver mis reservas</Link>
        <Link to="/">Volver al inicio</Link>
      </div>
    </div>
  )
}
