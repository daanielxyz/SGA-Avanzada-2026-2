package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El titular en el cuerpo de una reserva: nombre, documento y al menos un contacto, que exige el dominio (DEC-49).
 * Si ya existe con ese documento se reutiliza y se actualiza (DEC-58).
 */
public record TitularReservaRequest(
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

    public TitularCommand aCommand() {
        return new TitularCommand(nombre, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
