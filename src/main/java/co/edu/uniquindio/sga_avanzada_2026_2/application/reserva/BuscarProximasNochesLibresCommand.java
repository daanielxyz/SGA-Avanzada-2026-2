package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;

/**
 * Entrada de {@link BuscarProximasNochesLibresUseCase}: desde qué día buscar y cuántas noches mirar hacia adelante.
 */
public record BuscarProximasNochesLibresCommand(String alojamientoId, LocalDate desde, int dias) {
}
