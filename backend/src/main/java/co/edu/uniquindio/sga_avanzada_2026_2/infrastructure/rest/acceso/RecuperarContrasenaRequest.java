package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/auth/recuperar-contrasena} (CU-05).
 */
public record RecuperarContrasenaRequest(
        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        String correo) {
}
