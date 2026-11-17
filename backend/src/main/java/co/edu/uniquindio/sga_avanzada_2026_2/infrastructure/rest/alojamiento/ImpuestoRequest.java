package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PUT /api/alojamiento/iva} (DEC-133 · DEC-141): si se cobra el IVA de la estancia, su porcentaje y
 * si las tarifas ya lo incluyen.
 *
 * @param incluidoEnTarifa si la tarifa ya trae el IVA ($ 119.000 = $ 100.000 + $ 19.000); si no llega, se suma
 */
public record ImpuestoRequest(
        @NotNull(message = "es obligatorio")
        Boolean cobraIva,
        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "va de 0 a 100")
        @Max(value = 100, message = "va de 0 a 100")
        Integer porcentaje,
        Boolean incluidoEnTarifa) {
}
