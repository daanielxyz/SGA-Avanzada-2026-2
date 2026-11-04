package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.RegistrarTitularCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/titulares}. Valida solo el formato (400); que traiga al menos un contacto lo valida el
 * dominio (409) (DEC-49).
 */
public record RegistrarTitularRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @Email(message = "no tiene formato de correo")
        @Size(max = 150, message = "admite máximo 150 caracteres")
        String correo,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono) {

    RegistrarTitularCommand aCommand(String alojamientoId) {
        return new RegistrarTitularCommand(alojamientoId, nombre, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
