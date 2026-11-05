package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.PagoPendienteResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal.PoliticaPortalResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON de {@code GET /api/reservas/{codigo}/pago-en-linea}: lo que falta para confirmar la reserva (DEC-91).
 *
 * @param pagarAntesDe hora de Colombia; si no se paga antes, la reserva vence
 * @param condiciones  la política congelada en la reserva: la que se le aplica, no la vigente (RN-13)
 */
public record PagoPendienteResponse(BigDecimal montoAPagar, LocalDateTime pagarAntesDe,
                                    PoliticaPortalResponse condiciones) {

    static PagoPendienteResponse de(PagoPendienteResult r) {
        return new PagoPendienteResponse(r.montoAPagar(), r.pagarAntesDe(), PoliticaPortalResponse.de(r.condiciones()));
    }
}
