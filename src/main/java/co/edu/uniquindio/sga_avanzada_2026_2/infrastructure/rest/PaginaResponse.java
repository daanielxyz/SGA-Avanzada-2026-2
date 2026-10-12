package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * JSON de una página de un listado (DEC-18): de 10 en 10.
 *
 * @param numero         empieza en 0
 * @param totalElementos total del listado completo, no solo de esta página
 */
public record PaginaResponse<T>(List<T> contenido, int numero, int tamano, long totalElementos, int totalPaginas) {

    /** Convierte cada {@code XResult} de la página en su {@code XResponse}. */
    public static <R, T> PaginaResponse<T> de(Pagina<R> pagina, Function<R, T> conversion) {
        return new PaginaResponse<>(pagina.contenido().stream().map(conversion).toList(), pagina.numero(),
                pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
