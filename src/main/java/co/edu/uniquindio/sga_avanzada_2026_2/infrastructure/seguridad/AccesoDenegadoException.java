package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * El usuario inició sesión, pero su rol no le permite lo que pide dentro de un endpoint al que sí puede entrar
 * (DEC-22), p. ej. autorizar un saldo pendiente (AUTC-01).
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
