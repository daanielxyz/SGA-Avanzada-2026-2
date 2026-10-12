package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReservaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/reservas} (CU-10 · CU-11). Valida solo el formato (400); disponibilidad, capacidad y las
 * demás reglas las valida el dominio (409). Las reservas de un canal externo entran por el contrato del canal
 * (DEC-62).
 *
 * @param salida              exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes           el grupo completo, incluido el titular con su documento (TIT-01)
 * @param horaEstimadaLlegada opcional al crear; necesaria para confirmar (RN-09)
 */
public record CrearReservaRequest(
        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "PORTAL|DIRECTO", message = "debe ser PORTAL o DIRECTO")
        String canalOrigen,

        @NotNull(message = "es obligatorio")
        @Valid
        TitularReservaRequest titular,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid @NotNull(message = "no puede ser nulo") OcupanteRequest> ocupantes,

        LocalTime horaEstimadaLlegada) {

    CrearReservaCommand aCommand() {
        return new CrearReservaCommand(apartamento, entrada, salida, canalOrigen, titular.aCommand(),
                OcupanteRequest.aCommands(ocupantes), horaEstimadaLlegada);
    }
}
