package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/auth/login}.
 */
public record IniciarSesionRequest(
        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        String correo,

        @NotBlank(message = "es obligatoria")
        String contrasena) {

    /** Sin la contraseña, por si el Request termina en un log. */
    @Override
    public String toString() {
        return "IniciarSesionRequest[correo=" + correo + "]";
    }
}
