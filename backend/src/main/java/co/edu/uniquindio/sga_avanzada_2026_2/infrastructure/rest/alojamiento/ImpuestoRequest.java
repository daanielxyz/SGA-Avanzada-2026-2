package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PUT /api/alojamiento/iva} (DEC-133): si se cobra el IVA de la estancia y su porcentaje.
 */
public record ImpuestoRequest(
        @NotNull(message = "es obligatorio")
        Boolean cobraIva,
        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "va de 0 a 100")
        @Max(value = 100, message = "va de 0 a 100")
        Integer porcentaje) {
}
