package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.IvaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Salida de {@link CotizarEstanciaUseCase}: el desglose noche por noche y el total (COT-02). También es el valor
 * congelado de una reserva.
 *
 * @param total         la suma de las noches: la base, sin IVA (RN-05)
 * @param ivaPorcentaje el del alojamiento; 0 si no cobra IVA (DEC-134)
 * @param ivaExento     la reserva no paga IVA porque decide un pasaporte; en una cotización, siempre falso
 * @param iva           el IVA sobre {@code total}, redondeado al peso
 * @param totalConIva   {@code total} más {@code iva}: lo que ve el huésped como valor de la estancia
 */
public record CotizacionResult(List<NocheResult> desglose, BigDecimal total, int ivaPorcentaje, boolean ivaExento,
                               BigDecimal iva, BigDecimal totalConIva) {

    /**
     * @param temporada       id de la temporada de esa noche
     * @param temporadaNombre su nombre, congelado con la reserva (#10 · DEC-117)
     */
    public record NocheResult(LocalDate noche, String temporada, String temporadaNombre, BigDecimal tarifa,
                              int ocupantesFacturables, BigDecimal subtotal) {

        static NocheResult de(LineaCotizacion linea) {
            return new NocheResult(linea.noche().fecha(), linea.temporadaId().valor(), linea.temporadaNombre(),
                    linea.tarifa().monto(), linea.ocupantesFacturables(), linea.subtotal().monto());
        }
    }

    /** Una cotización, antes de saber quién llega: con el IVA si el alojamiento lo cobra (DEC-134). */
    static CotizacionResult de(Cotizacion cotizacion, IvaReserva iva) {
        return de(cotizacion.desglose(), cotizacion.total(), iva);
    }

    /** El valor congelado de una reserva, con su IVA congelado (DEC-134). */
    static CotizacionResult de(Reserva reserva) {
        return de(reserva.desglose(), reserva.valorTotal(), reserva.ivaReserva());
    }

    private static CotizacionResult de(List<LineaCotizacion> desglose, Dinero total, IvaReserva iva) {
        Dinero valorIva = iva.sobre(total);
        return new CotizacionResult(desglose.stream().map(NocheResult::de).toList(), total.monto(),
                iva.porcentaje().valor(), iva.exento(), valorIva.monto(), total.sumar(valorIva).monto());
    }
}
