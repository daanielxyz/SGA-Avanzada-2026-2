package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SimulacionCancelacion;

import java.math.BigDecimal;

/**
 * Salida de {@link SimularCancelacionUseCase} (DEC-91) y de {@link SimularNoShowUseCase} (DEC-102): lo que pasaría en
 * este momento.
 *
 * @param situacionSaldo PENDIENTE (debería el huésped), A_FAVOR (habría que devolver) o AL_DIA
 */
public record SimulacionCancelacionResult(BigDecimal retenido, BigDecimal saldo, String situacionSaldo) {

    static SimulacionCancelacionResult de(SimulacionCancelacion simulacion) {
        return new SimulacionCancelacionResult(simulacion.retenido().monto(), simulacion.saldo().monto().monto(),
                simulacion.saldo().situacion().name());
    }
}
