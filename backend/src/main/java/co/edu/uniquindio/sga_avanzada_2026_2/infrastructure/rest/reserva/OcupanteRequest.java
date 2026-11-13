package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Un ocupante del grupo en el cuerpo de una reserva. El documento es opcional al reservar; el TRA lo pide antes de
 * la salida (BL-07). {@code principal} marca al huésped principal; si nadie lo trae, lo es el primer mayor de edad
 * (DEC-97).
 */
public record OcupanteRequest(
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotNull(message = "es obligatoria")
        LocalDate fechaNacimiento,

        @Pattern(regexp = "CC|CE|PASAPORTE|TI|RC", message = "debe ser CC, CE, PASAPORTE, TI o RC")
        String tipoDocumento,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String nacionalidad,

        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombres,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String primerApellido,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String segundoApellido,

        Boolean principal) {

    /** Con los campos de antes de BL-07: el nombre completo en un solo texto. */
    public OcupanteRequest(String nombre, LocalDate fechaNacimiento, String tipoDocumento, String numeroDocumento,
                           String nacionalidad) {
        this(nombre, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad, null, null, null, null);
    }

    /** BL-07: los nombres llegan aparte de los apellidos o, como antes, todo junto en {@code nombre}. */
    @JsonIgnore
    @AssertTrue(message = "hace falta nombres o nombre")
    public boolean isNombreRecibido() {
        return (nombres != null && !nombres.isBlank()) || (nombre != null && !nombre.isBlank());
    }

    OcupanteCommand aCommand() {
        boolean separado = nombres != null && !nombres.isBlank();
        return new OcupanteCommand(separado ? nombres : nombre, separado ? primerApellido : null,
                separado ? segundoApellido : null, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad,
                Boolean.TRUE.equals(principal));
    }

    public static List<OcupanteCommand> aCommands(List<OcupanteRequest> ocupantes) {
        return ocupantes.stream().map(OcupanteRequest::aCommand).toList();
    }
}
