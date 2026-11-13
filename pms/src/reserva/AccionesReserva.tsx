import type { Alojamiento } from '../api/alojamiento'
import type { Reserva } from '../api/reservas'
import { fechaHoy, fechaSinAnio, hora, sumarDias } from '../formato'

export type Accion = 'confirmar' | 'cancelar' | 'noShow' | 'salida' | 'llegada' | 'corregir'

/** RES-16: el no-show se declara desde la hora límite del día de entrada. Si no se conoce, decide el backend. */
function noShowDisponibleDesde({ entrada }: Reserva, alojamiento: Alojamiento | null): string | null {
  const limite = alojamiento?.parametros.horaLimiteNoShow
  if (!limite || new Date() >= new Date(`${entrada}T${limite}`)) return null
  return `Disponible desde ${fechaSinAnio(entrada)}, ${hora(limite)}`
}

/**
 * DEC-110: una reserva pendiente no se confirma después de la hora límite de llegada de su día de entrada, salvo que
 * se haya creado después (la de mostrador tarde). Sin los parámetros, solo se sabe si el día ya pasó.
 */
function llegadaVencida({ entrada, creadaEn }: Reserva, alojamiento: Alojamiento | null, ahora = new Date()): string | null {
  const limite = alojamiento?.parametros.horaLimiteNoShow
  if (!limite) return fechaHoy(ahora) > entrada ? entrada : null
  const momento = new Date(`${entrada}T${limite}`)
  return ahora > momento && new Date(creadaEn) < momento ? `${fechaSinAnio(entrada)}, ${hora(limite)}` : null
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
  // #24: desde la víspera ya se puede abrir la llegada para adelantar el TRA; registrarla sigue siendo desde la entrada
  const entraManana = reserva.entrada === sumarDias(fechaHoy(), 1)
  if (reserva.estado === 'PENDIENTE') {
    const vencida = llegadaVencida(reserva, alojamiento)
    if (vencida) {
      return (
        <div className="acciones-reserva">
          <div className="acciones-reserva-noshow">
            <button type="button" className="boton boton-peligro-borde" aria-describedby="llegada-vencida" onClick={() => alElegir('cancelar')}>
              Cancelar
            </button>
            <p className="campo-pista" id="llegada-vencida">
              No se confirmó antes de la hora límite de llegada ({vencida}). Si el huésped llegó, cancélala y haz una nueva.
            </p>
          </div>
        </div>
      )
    }
    // Una reserva de mostrador entra hoy: se confirma y se registra desde la llegada
    const entraHoy = reserva.entrada === fechaHoy()
    return (
      <div className="acciones-reserva">
        {entraManana && (
          <div className="acciones-reserva-noshow">
            <button type="button" className="boton" disabled aria-describedby="llegada-desde">
              Registrar llegada
            </button>
            <p className="campo-pista" id="llegada-desde">
              Disponible desde {fechaSinAnio(reserva.entrada)}, ya confirmada
            </p>
          </div>
        )}
        {(entraHoy || entraManana) && (
          <button type="button" className={entraHoy ? 'boton' : 'boton boton-secundario'} onClick={() => alElegir('llegada')}>
            {entraHoy ? 'Continuar llegada' : 'Preparar llegada'}
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
        {entraManana && (
          <button type="button" className="boton boton-secundario" onClick={() => alElegir('llegada')}>
            Preparar llegada
          </button>
        )}
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
