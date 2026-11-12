import type { Alojamiento } from '../api/alojamiento'
import type { Reserva } from '../api/reservas'
import { fechaHoy, fechaSinAnio, hora } from '../formato'

export type Accion = 'confirmar' | 'cancelar' | 'noShow' | 'salida' | 'llegada' | 'corregir'

/** RES-16: el no-show se declara desde la hora límite del día de entrada. Si no se conoce, decide el backend. */
function noShowDisponibleDesde({ entrada }: Reserva, alojamiento: Alojamiento | null): string | null {
  const limite = alojamiento?.parametros.horaLimiteNoShow
  if (!limite || new Date() >= new Date(`${entrada}T${limite}`)) return null
  return `Disponible desde ${fechaSinAnio(entrada)}, ${hora(limite)}`
}

/** RN-10: la llegada se registra desde la fecha de entrada. */
function llegadaDisponibleDesde({ entrada }: Reserva): string | null {
  return fechaHoy() < entrada ? `Disponible desde ${fechaSinAnio(entrada)}` : null
}

/** Lo que se puede hacer según el estado (RN-08). Una reserva sin acciones no muestra nada. */
export default function AccionesReserva({ reserva, alojamiento, salidaDisponible = true, esAdministrador = false, alElegir }: {
  reserva: Reserva
  alojamiento: Alojamiento | null
  /** La salida necesita el folio: si no cargó, el bloque Pago ya ofrece «Reintentar». */
  salidaDisponible?: boolean
  /** Solo el administrador corrige el registro de llegada. */
  esAdministrador?: boolean
  alElegir: (accion: Accion) => void
}) {
  if (reserva.estado === 'PENDIENTE') {
    // Una reserva de mostrador entra hoy: se confirma y se registra desde la llegada
    const entraHoy = reserva.entrada === fechaHoy()
    return (
      <div className="acciones-reserva">
        {entraHoy && (
          <button type="button" className="boton" onClick={() => alElegir('llegada')}>
            Continuar llegada
          </button>
        )}
        <button type="button" className={entraHoy ? 'boton boton-secundario' : 'boton'} onClick={() => alElegir('confirmar')}>
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
    const llegadaDesde = llegadaDisponibleDesde(reserva)
    return (
      <div className="acciones-reserva">
        <div className="acciones-reserva-noshow">
          <button
            type="button"
            className="boton"
            disabled={Boolean(llegadaDesde)}
            aria-describedby={llegadaDesde ? 'llegada-desde' : undefined}
            onClick={() => alElegir('llegada')}
          >
            Registrar llegada
          </button>
          {llegadaDesde && <p className="campo-pista" id="llegada-desde">{llegadaDesde}</p>}
        </div>
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
  if (reserva.estado === 'EN_CURSO') {
    return (
      <div className="acciones-reserva">
        {esAdministrador && (
          <button type="button" className="boton boton-secundario" onClick={() => alElegir('corregir')}>
            Corregir registro
          </button>
        )}
        <button type="button" className="boton" disabled={!salidaDisponible} onClick={() => alElegir('salida')}>
          Registrar salida
        </button>
      </div>
    )
  }
  return null
}
