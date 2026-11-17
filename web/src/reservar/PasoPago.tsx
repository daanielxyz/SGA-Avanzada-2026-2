import { useEffect, useState } from 'react'
import { explicar } from '../api/mensajes'
import {
  consultarPolitica,
  pagar,
  type PagoAprobado,
  type PoliticaCancelacion,
  type ReservaApartada,
} from '../api/reservas'
import { valorEstancia } from '../busqueda/iva'
import { enPesos, fechaHora } from '../formato'
import Condiciones from './Condiciones'

// La pasarela de hoy es simulada (DEC-69): aprueba cualquier token, salvo el que sirve para probar un rechazo.
// TODO(equipo): con Wompi (BL-01), el token lo entrega su formulario y estos dos botones desaparecen.
const TOKEN_APROBADO = 'tok_prueba'
const TOKEN_RECHAZADO = 'tok_rechazado'

interface Props {
  apartada: ReservaApartada
  alPagar: (pago: PagoAprobado) => void
  /** Lo ya abonado, si se retoma el pago de una reserva con algún pago. */
  yaPagado?: number
  /** Las de esa reserva, si ya se conocen; si no, las vigentes, que son las que acaba de aceptar al reservar. */
  condiciones?: PoliticaCancelacion
}

/** Paso 2: pagar lo que exige el hotel, con las condiciones de cancelación a la vista. Si falla, se reintenta. */
export default function PasoPago({ apartada, alPagar, yaPagado = 0, condiciones }: Props) {
  const [vigentes, setVigentes] = useState<PoliticaCancelacion | null>(null)
  const politica = condiciones ?? vigentes
  const conoceCondiciones = condiciones !== undefined
  const [pagando, setPagando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)
  const { reserva, montoAPagar, pagarAntesDe, clave } = apartada
  const resto = valorEstancia(reserva.valor) + reserva.extras.total - yaPagado - montoAPagar

  useEffect(() => {
    if (conoceCondiciones) return
    let vigente = true
    // Si no carga, se paga igual: las condiciones también llegan por correo
    consultarPolitica().then((p) => vigente && setVigentes(p)).catch(() => {})
    return () => {
      vigente = false
    }
  }, [conoceCondiciones])

  async function intentar(token: string) {
    setProblema(null)
    setPagando(true)
    try {
      alPagar(await pagar(reserva.codigo, token, clave))
    } catch (error) {
      setProblema(explicar(error, 'No pudimos procesar el pago. Intenta de nuevo.'))
      setPagando(false)
    }
  }

  return (
    <div className="paso-pago">
      <p>
        Tu reserva <strong>{reserva.codigo}</strong> está apartada. Paga antes del{' '}
        <strong>{fechaHora(pagarAntesDe)}</strong> o las noches vuelven a quedar libres.
      </p>

      <dl className="cuentas">
        <dt>Pagas ahora</dt>
        <dd><strong>{enPesos(montoAPagar)}</strong></dd>
        {resto > 0 && (
          <>
            <dt>Pagas al llegar</dt>
            <dd>{enPesos(resto)}</dd>
          </>
        )}
      </dl>

      {politica && <Condiciones politica={politica} />}

      <div className="pago-prueba">
        <p>
          <strong>Pago de prueba.</strong> Esta versión no cobra nada ni pide datos de tarjeta.
        </p>
        {problema && <p role="alert" className="aviso">{problema}</p>}
        <button type="button" className="accion-principal" disabled={pagando}
          onClick={() => intentar(TOKEN_APROBADO)}>
          {pagando ? 'Procesando el pago…' : `Pagar ${enPesos(montoAPagar)}`}
        </button>
        <button type="button" className="secundario" disabled={pagando} onClick={() => intentar(TOKEN_RECHAZADO)}>
          Simular un pago rechazado
        </button>
      </div>
    </div>
  )
}
