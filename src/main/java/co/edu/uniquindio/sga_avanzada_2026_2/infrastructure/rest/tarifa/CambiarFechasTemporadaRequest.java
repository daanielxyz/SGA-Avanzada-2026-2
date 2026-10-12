package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CambiarFechasTemporadaCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Cuerpo de {@code PUT /api/temporadas/{id}/fechas}.
 *
 * @param fechaFin inclusiva (TEM-01)
 */
public record CambiarFechasTemporadaRequest(
        @NotNull(message = "es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "es obligatoria")
        LocalDate fechaFin) {

    CambiarFechasTemporadaCommand aCommand(String alojamientoId, String temporadaId) {
        return new CambiarFechasTemporadaCommand(alojamientoId, temporadaId, fechaInicio, fechaFin);
    }
}
