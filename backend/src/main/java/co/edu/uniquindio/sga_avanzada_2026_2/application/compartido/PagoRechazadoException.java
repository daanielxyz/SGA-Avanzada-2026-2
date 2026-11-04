package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * La pasarela no aprobó el cobro (DEC-69). No se registró ningún pago y la reserva sigue PENDIENTE: se puede
 * intentar de nuevo mientras no venza.
 */
public class PagoRechazadoException extends RuntimeException {

    public PagoRechazadoException(String motivo) {
        super("La pasarela rechazó el pago: " + motivo);
    }
}
