package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

/**
 * Ciclo de vida de la reserva.
 * PENDIENTE → CONFIRMADA | CANCELADA · CONFIRMADA → EN_CURSO | CANCELADA | NO_SHOW · EN_CURSO → FINALIZADA.
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA,
    NO_SHOW
}
