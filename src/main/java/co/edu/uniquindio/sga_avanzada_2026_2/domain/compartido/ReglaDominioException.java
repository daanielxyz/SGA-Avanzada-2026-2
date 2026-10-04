package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * Base de toda violación de una regla de negocio (se traduce a 409 en el borde).
 */
public class ReglaDominioException extends RuntimeException {

    public ReglaDominioException(String mensaje) {
        super(mensaje);
    }
}
