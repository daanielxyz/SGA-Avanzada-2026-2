package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import java.time.LocalDate;

/**
 * Entrada de {@link CambiarFechasTemporada} (CU-33).
 *
 * @param fechaFin inclusiva (TEM-01)
 */
public record CambiarFechasTemporadaCommand(String alojamientoId, String temporadaId, LocalDate fechaInicio,
                                            LocalDate fechaFin) {
}
