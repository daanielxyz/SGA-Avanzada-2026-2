package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link PagarEnLinea}.
 *
 * @param tokenPago lo que la pasarela le entregó al cliente; nunca datos de la tarjeta (DEC-69)
 */
public record PagarEnLineaCommand(String codigoReserva, String tokenPago) {
}
