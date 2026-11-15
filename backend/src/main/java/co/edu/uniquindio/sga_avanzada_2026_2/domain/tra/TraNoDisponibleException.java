package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

/**
 * El Ministerio no recibió el TRA (DEC-130): falta el token, no responde o rechazó la petición. No es una regla de
 * negocio rota ni bloquea la operación: el envío queda CON_ERROR con este mensaje y se reintenta.
 */
public class TraNoDisponibleException extends RuntimeException {

    public TraNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
