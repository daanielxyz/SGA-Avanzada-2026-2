package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDateTime;

/**
 * Entrada de {@link CorregirRegistro} (REG-06).
 *
 * @param fechaHora cuándo llegó realmente el grupo
 * @param autor     recepcionista que corrige; en B3 sale del usuario autenticado (DEC-61)
 */
public record CorregirRegistroCommand(String codigo, LocalDateTime fechaHora, String motivo, String autor) {
}
