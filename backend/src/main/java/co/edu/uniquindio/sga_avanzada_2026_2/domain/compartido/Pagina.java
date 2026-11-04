package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Una página de un listado, propia del dominio para que los puertos no dependan de Spring (DEC-18). Los listados
 * van de {@link #TAMANO} en {@link #TAMANO}.
 *
 * @param numero          empieza en 0
 * @param totalElementos  total del listado completo, no solo de esta página
 */
public record Pagina<T>(List<T> contenido, int numero, int tamano, long totalElementos) {

    public static final int TAMANO = 10;

    public Pagina {
        Objects.requireNonNull(contenido, "contenido");
        if (numero < 0 || tamano < 1 || totalElementos < 0) {
            throw new ReglaDominioException("Página inválida: número ≥ 0, tamaño ≥ 1 y total ≥ 0");
        }
        contenido = List.copyOf(contenido);
    }

    /**
     * Corta de una lista ya completa la página pedida: para listados que se arman en memoria (DEC-18).
     *
     * @param numero empieza en 0; una página que no existe llega vacía
     */
    public static <T> Pagina<T> de(List<T> todos, int numero) {
        int desde = (int) Math.min((long) numero * TAMANO, todos.size());
        return new Pagina<>(todos.subList(desde, Math.min(desde + TAMANO, todos.size())), numero, TAMANO, todos.size());
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }

    /**
     * La misma página con cada elemento convertido, p. ej. del agregado a su {@code XResult}.
     */
    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(contenido.stream().map(conversion).toList(), numero, tamano, totalElementos);
    }
}
