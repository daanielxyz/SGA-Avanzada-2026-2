package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizacionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON de una cotización (DEC-31): el desglose noche por noche y el total (COT-02). También es el valor congelado
 * de una reserva.
 *
 * @param total         la suma de las noches, con las tarifas tal cual
 * @param alojamiento   {@code total} sin el IVA que traen dentro las tarifas (DEC-141)
 * @param ivaPorcentaje el del alojamiento; 0 si no cobra IVA (DEC-134)
 * @param ivaExento     no paga IVA porque decide un pasaporte; en una cotización, siempre falso
 * @param iva           el IVA que se cobra
 * @param ivaIncluido   si las tarifas ya traen el IVA (DEC-141)
 * @param totalConIva   lo que ve el huésped como valor de la estancia
 */
public record CotizacionResponse(List<NocheResponse> desglose, BigDecimal total, BigDecimal alojamiento,
                                 int ivaPorcentaje, boolean ivaExento, BigDecimal iva, boolean ivaIncluido,
                                 BigDecimal totalConIva) {

    /**
     * @param temporada       id de la temporada de esa noche
     * @param temporadaNombre su nombre, para que recepción no lea solo el id (#10 · DEC-117)
     * @param tarifa          valor por ocupante facturable esa noche
     */
    public record NocheResponse(LocalDate noche, String temporada, String temporadaNombre, BigDecimal tarifa,
                                int ocupantesFacturables, BigDecimal subtotal) {
    }

    static CotizacionResponse de(CotizacionResult r) {
        return new CotizacionResponse(
                r.desglose().stream()
                        .map(n -> new NocheResponse(n.noche(), n.temporada(), n.temporadaNombre(), n.tarifa(),
                                n.ocupantesFacturables(),
                                n.subtotal()))
                        .toList(),
                r.total(), r.alojamiento(), r.ivaPorcentaje(), r.ivaExento(), r.iva(), r.ivaIncluido(),
                r.totalConIva());
    }
}
