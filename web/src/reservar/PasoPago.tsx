import { useEffect, useState } from 'react'
import { ErrorApi } from '../api/cliente'
import {
  consultarPolitica,
  pagar,
  type PagoAprobado,
  type Penalizacion,
  type PoliticaCancelacion,
  type ReservaApartada,
} from '../api/reservas'
import { enPesos, fechaHora } from '../formato'

// La pasarela de hoy es simulada (DEC-69): aprueba cualquier token, salvo el que sirve para probar un rechazo.
// TODO(equipo): con Wompi (BL-01), el token lo entrega su formulario y estos dos botones desaparecen.
const TOKEN_APROBADO = 'tok_prueba'
const TOKEN_RECHAZADO = 'tok_rechazado'

interface Props {
  apartada: ReservaApartada
  alPagar: (pago: PagoAprobado) => void
}

/** Paso 2: pagar lo que exige el hotel, con las condiciones de cancelación a la vista. Si falla, se reintenta. */
export default function PasoPago({ apartada, alPagar }: Props) {
  const [politica, setPolitica] = useState<PoliticaCancelacion | null>(null)
  const [pagando, setPagando] = useState(false)
  const [problema, setProblema] = useState<string | null>(null)
  const { reserva, montoAPagar, pagarAntesDe, cargosAdicionales } = apartada
  const resto = reserva.valor.total + cargosAdicionales - montoAPagar

  useEffect(() => {
    let vigente = true
    // Si no carga, se paga igual: las condiciones también llegan por correo
    consultarPolitica().then((p) => vigente && setPolitica(p)).catch(() => {})
    return () => {
      vigente = false
    }
  }, [])

  async function intentar(token: string) {
    setProblema(null)
    setPagando(true)
    try {
      alPagar(await pagar(reserva.codigo, token))
    } catch (error) {
      setProblema(error instanceof ErrorApi ? error.message : 'No pudimos procesar el pago. Intenta de nuevo.')
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
      {cargosAdicionales > 0 && (
        <p className="suave">Lo de las mascotas ({enPesos(cargosAdicionales)}) se paga al llegar.</p>
      )}

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

function Condiciones({ politica }: { politica: PoliticaCancelacion }) {
  // De la mayor antelación a la menor: se lee en el orden en que pasa el tiempo
  const tramos = [...politica.tramos].sort((a, b) => b.antelacionMinHoras - a.antelacionMinHoras)
  return (
    <section aria-labelledby="titulo-condiciones" className="condiciones">
      <h2 id="titulo-condiciones">Si necesitas cancelar</h2>
      <ul>
        {tramos.map((t, i) => (
          <li key={t.antelacionMinHoras}>
            <strong>{plazo(t.antelacionMinHoras, tramos[i - 1]?.antelacionMinHoras)}:</strong>{' '}
            {retencion(t.penalizacion)}
          </li>
        ))}
        <li><strong>Si no llegas:</strong> {retencion(politica.penalizacionNoShow)}</li>
      </ul>
    </section>
  )
}

// Cada tramo vale desde su antelación mínima hasta la del tramo anterior, que es mayor
function plazo(horas: number, horasDelAnterior: number | undefined) {
  if (horasDelAnterior === undefined) {
    return horas > 0 ? `Con ${horas} horas o más de antelación` : 'Hasta la llegada'
  }
  return horas > 0 ? `Entre ${horas} y ${horasDelAnterior} horas antes` : `Con menos de ${horasDelAnterior} horas`
}

const SOBRE: Record<Penalizacion['base'], string> = {
  VALOR_TOTAL: 'del total de la reserva',
  PAGADO: 'de lo que hayas pagado',
  ANTICIPO_EXIGIDO: 'del anticipo',
}

// DEC-41: cada hotel la define por porcentaje o por monto fijo
function retencion({ base, porcentaje, montoFijo }: Penalizacion) {
  if (montoFijo !== null) {
    return montoFijo > 0 ? `se retienen ${enPesos(montoFijo)}.` : 'no se retiene nada.'
  }
  return porcentaje ? `se retiene el ${porcentaje} % ${SOBRE[base]}.` : 'no se retiene nada.'
}
