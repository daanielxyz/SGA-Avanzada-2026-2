package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link EscribirNotaUseCase}. El autor sale de la sesión, nunca del Request (DEC-67).
 */
public record EscribirNotaCommand(String codigoReserva, String texto, String autor) {
}
