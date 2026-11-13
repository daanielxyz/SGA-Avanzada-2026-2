package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CambiarExtrasCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/extras} (R3 · DEC-116): los vehículos y las mascotas como quedan, los
 * tres obligatorios (DEC-38).
 */
public record CambiarExtrasRequest(
        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer carros,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer motos,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer mascotas) {

    CambiarExtrasCommand aCommand(String codigo) {
        return new CambiarExtrasCommand(codigo, carros, motos, mascotas);
    }
}
