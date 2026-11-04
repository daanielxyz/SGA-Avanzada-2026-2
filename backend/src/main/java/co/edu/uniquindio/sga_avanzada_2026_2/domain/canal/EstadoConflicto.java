package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

/**
 * Ciclo de vida del conflicto de canal: nace PENDIENTE y solo el administrador lo marca RESUELTO (CONF-04).
 */
public enum EstadoConflicto {
    PENDIENTE,
    RESUELTO
}
