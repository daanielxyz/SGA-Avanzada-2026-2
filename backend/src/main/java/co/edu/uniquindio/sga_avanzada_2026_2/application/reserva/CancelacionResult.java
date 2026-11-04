package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.math.BigDecimal;

/**
 * Salida de cancelar, declarar no-show o vencer una reserva: lo retenido y cómo queda el folio (DEC-43).
 *
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 */
public record CancelacionResult(String codigo, String estado, BigDecimal retenido, BigDecimal saldo,
                                String situacionSaldo) {

    static CancelacionResult de(Reserva reserva, Dinero retenido, Folio folio) {
        return new CancelacionResult(reserva.codigo().valor(), reserva.estado().name(), retenido.monto(),
                folio.saldo().monto().monto(), folio.saldo().situacion().name());
    }
}
