package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta de {@code POST /api/portal/reservas/{codigo}/pago}: lo justo para confirmarle el pago a quien paga. Este
 * endpoint es público y los códigos son consecutivos, así que no lleva ocupantes, documentos ni datos del titular.
 */
public record PagoPortalResponse(String codigo, String estado, String apartamento, LocalDate entrada,
                                 LocalDate salida, BigDecimal total) {

    static PagoPortalResponse de(ReservaResult r) {
        return new PagoPortalResponse(r.codigo(), r.estado(), r.apartamento(), r.entrada(), r.salida(),
                r.valor().total());
    }
}
