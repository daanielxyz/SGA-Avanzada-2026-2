package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.SimulacionCancelacionResult;

import java.math.BigDecimal;

/**
 * JSON de {@code GET /api/reservas/{codigo}/cancelacion}: lo que pasaría al cancelar en este momento (DEC-91).
 *
 * @param situacionSaldo PENDIENTE (debería el huésped), A_FAVOR (habría que devolver) o AL_DIA
 */
public record SimulacionCancelacionResponse(BigDecimal retenido, BigDecimal saldo, String situacionSaldo) {

    static SimulacionCancelacionResponse de(SimulacionCancelacionResult r) {
        return new SimulacionCancelacionResponse(r.retenido(), r.saldo(), r.situacionSaldo());
    }
}
