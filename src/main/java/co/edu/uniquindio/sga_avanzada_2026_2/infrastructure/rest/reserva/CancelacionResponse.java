package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelacionResult;

import java.math.BigDecimal;

/**
 * JSON de una cancelación o un no-show (DEC-31): lo retenido según la política congelada y cómo queda el folio.
 *
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 */
public record CancelacionResponse(String codigo, String estado, BigDecimal retenido, BigDecimal saldo,
                                  String situacionSaldo) {

    static CancelacionResponse de(CancelacionResult r) {
        return new CancelacionResponse(r.codigo(), r.estado(), r.retenido(), r.saldo(), r.situacionSaldo());
    }
}
