package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Ya existe un agregado con el mismo código de negocio. El borde la traduce a 409 (DEC-33).
 */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String recurso, String id) {
        super("Ya existe " + recurso + " " + id);
    }
}
