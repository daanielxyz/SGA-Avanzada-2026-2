package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Puerto hacia las claves de las reservas del portal (DEC-93): lo que prueba, sin cuenta, que quien paga o libera una
 * reserva es quien la hizo. Los códigos de reserva son consecutivos y se adivinan; la clave no.
 */
public interface ClavesReserva {

    /**
     * Crea la clave de la reserva. Se guarda solo su huella: la clave en claro se entrega esta única vez.
     *
     * @return la clave, para entregársela a quien reservó
     */
    String emitir(String codigoReserva);

    /** Si la clave es la de esa reserva; {@code false} si la reserva no tiene clave o no existe. */
    boolean corresponde(String codigoReserva, String clave);
}
