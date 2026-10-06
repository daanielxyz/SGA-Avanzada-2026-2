package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import java.util.Objects;

/**
 * Ciclo de vida de la reserva, conjunto cerrado de seis valores (EDO-01).
 * PENDIENTE → CONFIRMADA | CANCELADA · CONFIRMADA → EN_CURSO | CANCELADA | NO_SHOW · EN_CURSO → FINALIZADA.
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA,
    NO_SHOW;

    /**
     * Transiciones del ciclo de vida; los terminales (FINALIZADA, CANCELADA, NO_SHOW) no tienen salida
     * (RN-08 · EDO-02 · EDO-04).
     *
     * @param destino estado al que se quiere pasar
     * @return {@code true} si la transición está permitida
     */
    public boolean puedePasarA(EstadoReserva destino) {
        Objects.requireNonNull(destino, "destino");
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == EN_CURSO || destino == CANCELADA || destino == NO_SHOW;
            case EN_CURSO -> destino == FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;
        };
    }

    /**
     * Solo PENDIENTE, CONFIRMADA y EN_CURSO retienen noches; al salir de ellos las noches se liberan de inmediato
     * (EDO-03 · RN-01 · RN-12).
     *
     * @return {@code true} si la reserva cuenta para la disponibilidad
     */
    public boolean retieneDisponibilidad() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }
}
