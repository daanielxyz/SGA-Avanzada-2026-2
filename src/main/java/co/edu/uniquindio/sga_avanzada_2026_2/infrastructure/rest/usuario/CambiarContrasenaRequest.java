package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.PoliticaContrasena;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de {@code PUT /api/usuarios/perfil/contrasena} (CU-04): la actual y la nueva, que debe cumplir USU-02.
 */
public record CambiarContrasenaRequest(
        @NotBlank(message = "es obligatoria")
        String actual,

        @NotBlank(message = "es obligatoria")
        @Pattern(regexp = PoliticaContrasena.PATRON, message = PoliticaContrasena.DESCRIPCION)
        String nueva) {

    /** Sin las contraseñas, por si el Request termina en un log. */
    @Override
    public String toString() {
        return "CambiarContrasenaRequest[]";
    }
}
