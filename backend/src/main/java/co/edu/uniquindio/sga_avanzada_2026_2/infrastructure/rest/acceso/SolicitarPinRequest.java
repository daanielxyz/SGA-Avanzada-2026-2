package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/auth/huesped/pin}: el correo de la cuenta y el número de documento del titular.
 */
public record SolicitarPinRequest(
        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        String correo,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento) {
}
