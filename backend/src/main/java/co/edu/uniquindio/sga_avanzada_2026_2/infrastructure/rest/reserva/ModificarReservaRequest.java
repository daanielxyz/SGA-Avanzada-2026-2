package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ModificarReservaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/modificar} (CU-17): los datos completos como quedan, no solo lo que
 * cambia (DEC-38).
 *
 * @param apartamento el mismo u otro apartamento destino
 * @param salida      exclusiva (EST-01)
 * @param ocupantes   el grupo completo; el titular sigue siendo el mismo (TIT-01)
 */
public record ModificarReservaRequest(
        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid @NotNull(message = "no puede ser nulo") OcupanteRequest> ocupantes) {

    ModificarReservaCommand aCommand(String codigo) {
        return new ModificarReservaCommand(codigo, apartamento, entrada, salida,
                OcupanteRequest.aCommands(ocupantes));
    }
}
