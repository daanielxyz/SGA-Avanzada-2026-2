package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link AtenderNotaUseCase}. El autor sale de la sesión, nunca del Request (DEC-67).
 */
public record AtenderNotaCommand(String codigoReserva, String nota, String autor) {
}
