package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.RegistrarTitularCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/titulares}. Valida solo el formato (400); que traiga al menos un contacto lo valida el
 * dominio (409) (DEC-49). El nombre llega en {@code nombres} y los apellidos aparte o, como antes, todo junto en
 * {@code nombre} (DEC-98).
 */
public record RegistrarTitularRequest(
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
        String telefono,

        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombres,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String primerApellido,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String segundoApellido) {

    /** DEC-98: los nombres llegan aparte de los apellidos o, como antes, todo junto en {@code nombre}. */
    @JsonIgnore
    @AssertTrue(message = "hace falta nombres o nombre")
    public boolean isNombreRecibido() {
        return separado() || (nombre != null && !nombre.isBlank());
    }

    private boolean separado() {
        return nombres != null && !nombres.isBlank();
    }

    RegistrarTitularCommand aCommand(String alojamientoId) {
        return new RegistrarTitularCommand(alojamientoId, separado() ? nombres : nombre,
                separado() ? primerApellido : null, separado() ? segundoApellido : null, tipoDocumento,
                numeroDocumento, correo, telefono);
    }
}
