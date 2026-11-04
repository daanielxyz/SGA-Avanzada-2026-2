package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Un ocupante del grupo en el cuerpo de una reserva. El documento es opcional, salvo para el titular, que se
 * reconoce por él (TIT-01).
 */
public record OcupanteRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotNull(message = "es obligatoria")
        LocalDate fechaNacimiento,

        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String nacionalidad) {

    OcupanteCommand aCommand() {
        return new OcupanteCommand(nombre, fechaNacimiento, tipoDocumento, numeroDocumento, nacionalidad);
    }

    public static List<OcupanteCommand> aCommands(List<OcupanteRequest> ocupantes) {
        return ocupantes.stream().map(OcupanteRequest::aCommand).toList();
    }
}
