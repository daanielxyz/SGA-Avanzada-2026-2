package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Salida de {@link CotizarEstancia}: el desglose noche por noche y el total (COT-02). También es el valor congelado
 * de una reserva.
 */
public record CotizacionResult(List<NocheResult> desglose, BigDecimal total) {

    public record NocheResult(LocalDate noche, String temporada, BigDecimal tarifa, int ocupantesFacturables,
                              BigDecimal subtotal) {

        static NocheResult de(LineaCotizacion linea) {
            return new NocheResult(linea.noche().fecha(), linea.temporadaId().valor(), linea.tarifa().monto(),
                    linea.ocupantesFacturables(), linea.subtotal().monto());
        }
    }

    static CotizacionResult de(Cotizacion cotizacion) {
        return de(cotizacion.desglose(), cotizacion.total().monto());
    }

    static CotizacionResult de(List<LineaCotizacion> desglose, BigDecimal total) {
        return new CotizacionResult(desglose.stream().map(NocheResult::de).toList(), total);
    }
}
