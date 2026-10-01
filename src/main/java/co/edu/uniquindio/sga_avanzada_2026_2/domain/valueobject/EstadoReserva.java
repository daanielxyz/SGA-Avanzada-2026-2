package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

/**
 * Objeto de valor (Enum): Estado del ciclo de vida de una Reserva.
 */
public enum EstadoReserva {
    PENDIENTE(true),
    CONFIRMADA(true),
    EN_CURSO(true),
    FINALIZADA(false),
    CANCELADA(false),
    NO_SHOW(false);

    private final boolean activa;

    EstadoReserva(boolean activa) {
        this.activa = activa;
    }

    /**
     * Consulta si la reserva se encuentra activa.
     */
    public boolean estaActiva() {
        return activa;
    }

    /**
     * Determina si el estado es final/terminal.
     */
    public boolean esTerminal() {
        return !activa;
    }

    /**
     * Indica si la reserva en este estado bloquea o retiene la disponibilidad de fechas.
     */
    public boolean retieneDisponibilidad() {
        return activa;
    }

    /**
     * Valida si la transición hacia el nuevo estado está permitida por las reglas de negocio.
     */
    public boolean puedePasarA(EstadoReserva destino) {
        if (destino == null || this == destino) {
            return false;
        }
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == EN_CURSO || destino == CANCELADA || destino == NO_SHOW;
            case EN_CURSO -> destino == FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;
        };
    }
}
