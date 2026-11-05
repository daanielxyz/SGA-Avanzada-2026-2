package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SimulacionCancelacion;

import java.math.BigDecimal;

/**
 * Salida de {@link SimularCancelacionUseCase}: lo que pasaría al cancelar en este momento (DEC-91).
 *
 * @param situacionSaldo PENDIENTE (debería el huésped), A_FAVOR (habría que devolver) o AL_DIA
 */
public record SimulacionCancelacionResult(BigDecimal retenido, BigDecimal saldo, String situacionSaldo) {

    static SimulacionCancelacionResult de(SimulacionCancelacion simulacion) {
        return new SimulacionCancelacionResult(simulacion.retenido().monto(), simulacion.saldo().monto().monto(),
                simulacion.saldo().situacion().name());
    }
}
