package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.TarifaResult;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JSON de la tarifa vigente de un apartamento en una temporada (DEC-31): valor por ocupante facturable y noche.
 */
public record TarifaResponse(String id, String apartamento, String temporada, BigDecimal valorPorOcupante,
                             int version, LocalDate vigenteDesde) {

    static TarifaResponse de(TarifaResult r) {
        return new TarifaResponse(r.id(), r.apartamento(), r.temporada(), r.valorPorOcupante(), r.version(),
                r.vigenteDesde());
    }
}
