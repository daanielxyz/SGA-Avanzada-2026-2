package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.SimulacionCancelacionResult;

import java.math.BigDecimal;

/**
 * JSON de {@code GET /api/reservas/{codigo}/cancelacion} (DEC-91) y {@code GET /api/reservas/{codigo}/no-show}
 * (DEC-102): lo que pasaría en este momento.
 *
 * @param situacionSaldo PENDIENTE (debería el huésped), A_FAVOR (habría que devolver) o AL_DIA
 */
public record SimulacionCancelacionResponse(BigDecimal retenido, BigDecimal saldo, String situacionSaldo) {

    static SimulacionCancelacionResponse de(SimulacionCancelacionResult r) {
        return new SimulacionCancelacionResponse(r.retenido(), r.saldo(), r.situacionSaldo());
    }
}
