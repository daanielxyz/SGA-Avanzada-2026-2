package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaPortalResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.ReservaResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta de {@code POST /api/portal/reservas}: la reserva PENDIENTE y lo que falta para confirmarla (DEC-69).
 *
 * @param montoAPagar  lo que se cobra en línea
 * @param pagarAntesDe hora de Colombia; si no se paga antes, la reserva vence
 */
public record ReservaPortalResponse(ReservaResponse reserva, BigDecimal montoAPagar, LocalDateTime pagarAntesDe) {

    static ReservaPortalResponse de(ReservaPortalResult r) {
        return new ReservaPortalResponse(ReservaResponse.de(r.reserva()), r.montoAPagar(), r.pagarAntesDe());
    }
}
