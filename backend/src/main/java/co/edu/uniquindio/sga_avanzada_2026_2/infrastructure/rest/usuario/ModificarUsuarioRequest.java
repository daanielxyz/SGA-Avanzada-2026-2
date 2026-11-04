package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo de {@code PUT /api/usuarios/{id}}: datos y rol (ROL-03). El correo no se edita (USU-01).
 */
public record ModificarUsuarioRequest(
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
        LocalDate fechaNacimiento,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "RECEPCIONISTA|PERSONAL_SERVICIO|ADMINISTRADOR",
                message = "debe ser RECEPCIONISTA, PERSONAL_SERVICIO o ADMINISTRADOR")
        String rol) {

    Usuario.DatosPersonales aDatos() {
        return DatosPersonalesRequest.datos(nombre, telefono, tipoDocumento, numeroDocumento, fechaNacimiento);
    }
}
