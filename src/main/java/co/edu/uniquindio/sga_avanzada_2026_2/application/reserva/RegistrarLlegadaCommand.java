package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link RegistrarLlegada}.
 *
 * @param autor recepcionista que registra (REG-05); es el usuario autenticado (DEC-61 · DEC-67)
 */
public record RegistrarLlegadaCommand(String codigo, String autor) {
}
