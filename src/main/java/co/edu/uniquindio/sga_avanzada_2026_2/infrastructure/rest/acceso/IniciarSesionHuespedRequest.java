package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/auth/huesped/login}: los mismos datos de la solicitud del PIN y el PIN que llegó.
 */
public record IniciarSesionHuespedRequest(
        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        String correo,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos")
        String pin) {

    /** Sin el PIN, por si el Request termina en un log. */
    @Override
    public String toString() {
        return "IniciarSesionHuespedRequest[correo=" + correo + "]";
    }
}
