package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarCapacidadCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PUT /api/apartamentos/{codigo}/capacidad} (APA-15).
 */
public record CambiarCapacidadRequest(
        @NotNull(message = "es obligatoria")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer capacidad) {

    CambiarCapacidadCommand aCommand(String codigo) {
        return new CambiarCapacidadCommand(codigo, capacidad);
    }
}
