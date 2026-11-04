package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;

/**
 * Entrada de {@link CotizarEstanciaUseCase} cuando todavía no se conoce a los ocupantes (CU-09, portal): basta cuántos
 * alcanzan el umbral de edad a la fecha de entrada, porque solo ellos pagan (RN-06).
 */
public record CotizarPorCantidadCommand(String apartamento, LocalDate entrada, LocalDate salida, int facturables) {
}
