package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.IntegracionExterna;

import java.util.Optional;

/**
 * Salida de {@link RecibirReservaExterna} (DEC-31), la respuesta para el canal.
 *
 * @param resultado     EXITOSO (reserva nueva), DUPLICADO (ya se había recibido, RN-19) o CONFLICTO (RN-18)
 * @param codigoReserva la reserva nueva o la que ya existía; vacío en un conflicto
 * @param conflictoId   solo en un conflicto
 * @param detalle       el código de la reserva o el motivo del conflicto
 */
public record IntegracionExternaResult(String resultado, String codigoReserva, String conflictoId, String detalle) {

    static IntegracionExternaResult de(IntegracionExterna integracion) {
        return new IntegracionExternaResult(integracion.resultado().name(),
                Optional.ofNullable(integracion.reserva()).map(Reserva::codigo).map(c -> c.valor()).orElse(null),
                integracion.conflictoRegistrado().map(ConflictoCanal::id).map(c -> c.valor()).orElse(null),
                integracion.detalle());
    }
}
