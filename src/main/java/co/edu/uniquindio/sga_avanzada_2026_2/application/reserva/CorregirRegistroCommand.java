package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDateTime;

/**
 * Entrada de {@link CorregirRegistro} (REG-06).
 *
 * @param fechaHora cuándo llegó realmente el grupo
 * @param autor     administrador que corrige (CU-56); es el usuario autenticado (DEC-61 · DEC-67)
 */
public record CorregirRegistroCommand(String codigo, LocalDateTime fechaHora, String motivo, String autor) {
}
