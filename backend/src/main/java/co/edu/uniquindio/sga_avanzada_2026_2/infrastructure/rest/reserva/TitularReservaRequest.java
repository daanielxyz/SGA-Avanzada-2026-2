package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El titular en el cuerpo de una reserva del portal o de recepción: nombre, documento, correo y teléfono, todos
 * obligatorios porque con el correo el titular tiene cuenta y con el teléfono la recupera (DEC-68). Si ya existe con
 * ese documento se reutiliza y solo se completa el contacto que le falte (DEC-58 · DEC-69). El nombre llega en
 * {@code nombres} y los apellidos aparte o, como antes, todo junto en {@code nombre} (DEC-98).
 */
public record TitularReservaRequest(
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene formato de correo")
        @Size(max = 150, message = "admite máximo 150 caracteres")
        String correo,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono,

        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombres,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String primerApellido,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String segundoApellido) {

    /** Con los campos de antes de DEC-98: el nombre completo en un solo texto. */
    public TitularReservaRequest(String nombre, String tipoDocumento, String numeroDocumento, String correo,
                                 String telefono) {
        this(nombre, tipoDocumento, numeroDocumento, correo, telefono, null, null, null);
    }

    /** DEC-98: los nombres llegan aparte de los apellidos o, como antes, todo junto en {@code nombre}. */
    @JsonIgnore
    @AssertTrue(message = "hace falta nombres o nombre")
    public boolean isNombreRecibido() {
        return separado() || (nombre != null && !nombre.isBlank());
    }

    private boolean separado() {
        return nombres != null && !nombres.isBlank();
    }

    public TitularCommand aCommand() {
        return new TitularCommand(separado() ? nombres : nombre, separado() ? primerApellido : null,
                separado() ? segundoApellido : null, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
