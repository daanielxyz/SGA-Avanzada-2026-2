package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/hora-llegada} (RN-09).
 */
public record HoraLlegadaRequest(
        @NotNull(message = "es obligatoria")
        LocalTime hora) {

    IndicarHoraLlegadaCommand aCommand(String codigo) {
        return new IndicarHoraLlegadaCommand(codigo, hora);
    }
}
