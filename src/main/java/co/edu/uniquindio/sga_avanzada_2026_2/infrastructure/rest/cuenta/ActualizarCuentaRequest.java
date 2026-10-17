package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.cuenta;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarCuentaTitularCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/mi-cuenta}: nombre y teléfono. El correo cambia con PIN y el documento nunca (DEC-69).
 */
public record ActualizarCuentaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono) {

    ActualizarCuentaTitularCommand aCommand(String titularId) {
        return new ActualizarCuentaTitularCommand(titularId, nombre, telefono);
    }
}
