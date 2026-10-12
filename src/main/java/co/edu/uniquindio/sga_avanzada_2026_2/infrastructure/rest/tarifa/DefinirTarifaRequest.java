package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code PUT /api/apartamentos/{codigo}/tarifas/{temporada}} (CU-34): el valor rige desde hoy (DEC-54).
 */
public record DefinirTarifaRequest(
        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor que cero")
        @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
        BigDecimal valorPorOcupante) {

    DefinirTarifaCommand aCommand(String apartamento, String temporadaId) {
        return new DefinirTarifaCommand(apartamento, temporadaId, valorPorOcupante);
    }
}
