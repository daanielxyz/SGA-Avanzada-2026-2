package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;

import java.time.LocalDate;
import java.util.List;

/**
 * Salida de los casos de uso del calendario de temporadas (DEC-31).
 */
public record CalendarioResult(String alojamientoId, List<TemporadaResult> temporadas) {

    /** La base no tiene fechas (TEM-09). */
    public record TemporadaResult(String id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                  boolean esBase, int estanciaMinimaNoches, boolean activa) {

        static TemporadaResult de(Temporada t) {
            return new TemporadaResult(t.id().valor(), t.nombre(), t.fechaInicio(), t.fechaFin(), t.esBase(),
                    t.estanciaMinimaNoches(), t.activa());
        }
    }

    static CalendarioResult de(CalendarioTemporadas calendario) {
        return new CalendarioResult(calendario.alojamientoId().valor(),
                calendario.temporadas().stream().map(TemporadaResult::de).toList());
    }
}
