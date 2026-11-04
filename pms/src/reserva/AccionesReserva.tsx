import type { Alojamiento } from '../api/alojamiento'
import type { Reserva } from '../api/reservas'
import { fechaSinAnio, hora } from '../formato'

export type Accion = 'confirmar' | 'cancelar' | 'noShow'

/** RES-16: el no-show se declara desde la hora límite del día de entrada. Si no se conoce, decide el backend. */
function noShowDisponibleDesde({ entrada }: Reserva, alojamiento: Alojamiento | null): string | null {
  const limite = alojamiento?.parametros.horaLimiteNoShow
  if (!limite || new Date() >= new Date(`${entrada}T${limite}`)) return null
  return `Disponible desde ${fechaSinAnio(entrada)}, ${hora(limite)}`
}

/** Lo que se puede hacer según el estado (RN-08). Una reserva sin acciones no muestra nada. */
export default function AccionesReserva({ reserva, alojamiento, alElegir }: {
  reserva: Reserva
  alojamiento: Alojamiento | null
  alElegir: (accion: Accion) => void
}) {
  if (reserva.estado === 'PENDIENTE') {
    return (
      <div className="acciones-reserva">
        <button type="button" className="boton" onClick={() => alElegir('confirmar')}>
          Confirmar
        </button>
        <button type="button" className="boton boton-peligro-borde" onClick={() => alElegir('cancelar')}>
          Cancelar
        </button>
      </div>
    )
  }
  if (reserva.estado === 'CONFIRMADA') {
    const desde = noShowDisponibleDesde(reserva, alojamiento)
    return (
      <div className="acciones-reserva">
        <div className="acciones-reserva-noshow">
          <button
            type="button"
            className="boton boton-secundario"
            disabled={Boolean(desde)}
            aria-describedby={desde ? 'noshow-desde' : undefined}
            onClick={() => alElegir('noShow')}
          >
            Declarar no-show
          </button>
          {desde && <p className="campo-pista" id="noshow-desde">{desde}</p>}
        </div>
        <button type="button" className="boton boton-peligro-borde" onClick={() => alElegir('cancelar')}>
          Cancelar
        </button>
      </div>
    )
  }
  return null
}
