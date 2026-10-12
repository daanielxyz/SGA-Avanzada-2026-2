package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link RegistrarLlegada}.
 *
 * @param autor recepcionista que registra (REG-05); en B3 sale del usuario autenticado (DEC-61)
 */
public record RegistrarLlegadaCommand(String codigo, String autor) {
}
