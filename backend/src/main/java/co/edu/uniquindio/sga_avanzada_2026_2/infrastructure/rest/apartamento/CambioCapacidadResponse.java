package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambioCapacidadResult;

import java.util.List;

/**
 * JSON del cambio de capacidad (DEC-31): el apartamento y las reservas activas cuyo grupo ya no cabe, para que
 * recepción las revise (APA-15).
 */
public record CambioCapacidadResponse(ApartamentoResponse apartamento, List<String> reservasAfectadas) {

    static CambioCapacidadResponse de(CambioCapacidadResult r) {
        return new CambioCapacidadResponse(ApartamentoResponse.de(r.apartamento()), r.reservasAfectadas());
    }
}
