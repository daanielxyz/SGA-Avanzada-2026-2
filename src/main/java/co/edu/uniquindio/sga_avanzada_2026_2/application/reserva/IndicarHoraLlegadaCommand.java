package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalTime;

/**
 * Entrada de {@link IndicarHoraLlegada} (RN-09).
 */
public record IndicarHoraLlegadaCommand(String codigo, LocalTime hora) {
}
