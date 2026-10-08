package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;

/**
 * Entrada de {@link BuscarDisponibles} (CU-07): fechas y tamaño del grupo.
 */
public record BuscarDisponiblesCommand(String alojamientoId, LocalDate entrada, LocalDate salida, int ocupantes) {
}
