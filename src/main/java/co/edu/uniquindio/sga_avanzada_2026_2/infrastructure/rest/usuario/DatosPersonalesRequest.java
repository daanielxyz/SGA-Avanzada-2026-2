package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo de {@code PUT /api/usuarios/perfil}: los datos personales que pide USU-01, sin el correo, que no se edita, ni
 * el rol, que solo cambia el administrador (ROL-03).
 */
public record DatosPersonalesRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @NotNull(message = "es obligatoria")
        @Past(message = "debe ser anterior a hoy")
        LocalDate fechaNacimiento) {

    Usuario.DatosPersonales aDatos() {
        return datos(nombre, telefono, tipoDocumento, numeroDocumento, fechaNacimiento);
    }

    static Usuario.DatosPersonales datos(String nombre, String telefono, String tipoDocumento, String numeroDocumento,
                                         LocalDate fechaNacimiento) {
        return new Usuario.DatosPersonales(nombre, telefono, TipoDocumento.valueOf(tipoDocumento),
                numeroDocumento.trim(), fechaNacimiento);
    }
}
