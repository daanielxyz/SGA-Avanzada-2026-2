package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * El caso de uso no encontró el agregado pedido o uno que referencia. No es una regla de negocio: el borde la
 * traduce a 404 (DEC-33).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " " + id);
    }
}
