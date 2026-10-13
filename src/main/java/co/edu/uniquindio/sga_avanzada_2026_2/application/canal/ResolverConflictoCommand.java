package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

/**
 * Entrada de {@link ResolverConflicto} (CONF-04).
 *
 * @param decision qué se hizo con la reserva rechazada, en texto libre
 * @param autor    el administrador que la revisó; es el usuario autenticado (DEC-61 · DEC-67)
 */
public record ResolverConflictoCommand(String id, String decision, String autor) {
}
