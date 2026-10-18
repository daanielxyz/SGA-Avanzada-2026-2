package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.PoliticaContrasena;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de {@code POST /api/auth/activar} y {@code POST /api/auth/restablecer-contrasena} (CU-05 · DEC-71): el correo,
 * el código que llegó y la contraseña nueva, que debe cumplir USU-02.
 */
public record FijarContrasenaRequest(
        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        String correo,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos")
        String codigo,

        @NotBlank(message = "es obligatoria")
        @Pattern(regexp = PoliticaContrasena.PATRON, message = PoliticaContrasena.DESCRIPCION)
        String contrasena) {

    /** Sin el código ni la contraseña, por si el Request termina en un log. */
    @Override
    public String toString() {
        return "FijarContrasenaRequest[correo=" + correo + "]";
    }
}
