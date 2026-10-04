package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

/**
 * Estado operativo del apartamento (independiente del bloqueo).
 * PREPARADO → OCUPADO → PENDIENTE_PREPARACION → EN_PREPARACION → PREPARADO;
 * FUERA_DE_SERVICIO desde cualquier estado no ocupado (solo admin).
 */
public enum EstadoOperativo {
    PREPARADO,
    OCUPADO,
    PENDIENTE_PREPARACION,
    EN_PREPARACION,
    FUERA_DE_SERVICIO
}
