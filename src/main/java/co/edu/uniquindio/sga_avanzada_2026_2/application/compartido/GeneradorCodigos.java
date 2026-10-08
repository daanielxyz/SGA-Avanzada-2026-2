package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Puerto de salida de la aplicación: entrega códigos de negocio nuevos (DEC-52). Es de la aplicación y no del
 * dominio porque el dominio recibe los ids ya hechos.
 */
public interface GeneradorCodigos {

    /**
     * Código nuevo de la serie, p. ej. {@code TEM-7}. Nunca se repite, aunque dos transacciones lo pidan a la vez.
     */
    String siguiente(SerieCodigo serie);
}
