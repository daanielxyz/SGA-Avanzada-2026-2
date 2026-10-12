package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult;

import java.time.LocalDate;
import java.util.List;

/**
 * JSON del calendario de temporadas del alojamiento (DEC-31).
 */
public record CalendarioResponse(List<TemporadaResponse> temporadas) {

    /**
     * @param fechaInicio vacía en la temporada base, que no tiene fechas (TEM-09)
     * @param fechaFin    inclusiva; vacía en la temporada base
     */
    public record TemporadaResponse(String id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                    boolean esBase, int estanciaMinimaNoches, boolean activa) {

        static TemporadaResponse de(CalendarioResult.TemporadaResult t) {
            return new TemporadaResponse(t.id(), t.nombre(), t.fechaInicio(), t.fechaFin(), t.esBase(),
                    t.estanciaMinimaNoches(), t.activa());
        }
    }

    static CalendarioResponse de(CalendarioResult r) {
        return new CalendarioResponse(r.temporadas().stream().map(TemporadaResponse::de).toList());
    }
}
