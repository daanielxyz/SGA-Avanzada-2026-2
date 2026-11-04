package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizacionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON de una cotización (DEC-31): el desglose noche por noche y el total (COT-02). También es el valor congelado
 * de una reserva.
 */
public record CotizacionResponse(List<NocheResponse> desglose, BigDecimal total) {

    /**
     * @param tarifa valor por ocupante facturable esa noche
     */
    public record NocheResponse(LocalDate noche, String temporada, BigDecimal tarifa, int ocupantesFacturables,
                                BigDecimal subtotal) {
    }

    static CotizacionResponse de(CotizacionResult r) {
        return new CotizacionResponse(
                r.desglose().stream()
                        .map(n -> new NocheResponse(n.noche(), n.temporada(), n.tarifa(), n.ocupantesFacturables(),
                                n.subtotal()))
                        .toList(),
                r.total());
    }
}
