package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservarDesdePortalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.OcupanteRequest;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.TitularReservaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/portal/reservas} (CU-10). No trae canal (es siempre PORTAL) ni precio (lo calcula el
 * sistema); la hora de llegada es obligatoria porque pagar confirma la reserva (RN-09 · DEC-69).
 *
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes el grupo completo, incluido el titular con su documento (TIT-01)
 */
public record ReservaPortalRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 20, message = "admite máximo 20 caracteres")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotNull(message = "es obligatorio")
        @Valid
        TitularReservaRequest titular,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes,

        @NotNull(message = "es obligatoria")
        LocalTime horaEstimadaLlegada) {

    ReservarDesdePortalCommand aCommand() {
        return new ReservarDesdePortalCommand(apartamento, entrada, salida, titular.aCommand(),
                OcupanteRequest.aCommands(ocupantes), horaEstimadaLlegada);
    }
}
