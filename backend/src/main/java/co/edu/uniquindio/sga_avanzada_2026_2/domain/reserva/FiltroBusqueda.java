package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * VO: filtros de la búsqueda de apartamentos disponibles (CU-07). Todos son opcionales.
 *
 * @param capacidadMinima  el apartamento admite al menos estas personas
 * @param precioMinimo     valor estimado de la estancia, en COP
 * @param precioMaximo     valor estimado de la estancia, en COP
 * @param caracteristicas  el apartamento debe tenerlas todas, sin distinguir mayúsculas
 * @param servicios        el alojamiento debe ofrecerlos activos todos, sin distinguir mayúsculas
 */
public record FiltroBusqueda(Integer capacidadMinima, BigDecimal precioMinimo, BigDecimal precioMaximo,
                             List<String> caracteristicas, List<String> servicios) {

    public FiltroBusqueda {
        if (precioMinimo != null && precioMaximo != null && precioMinimo.compareTo(precioMaximo) > 0) {
            throw new ReglaDominioException("El precio mínimo no puede ser mayor que el máximo");
        }
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        servicios = servicios == null ? List.of() : List.copyOf(servicios);
    }

    /** Antes de calcular el precio: lo que depende solo del apartamento y del alojamiento. */
    public boolean admite(Apartamento apartamento, List<ServicioAdicional> ofrecidos) {
        List<String> dotacion = apartamento.caracteristicas().stream().map(Caracteristica::nombre)
                .map(FiltroBusqueda::normal).toList();
        List<String> activos = ofrecidos.stream().filter(ServicioAdicional::activo).map(ServicioAdicional::nombre)
                .map(FiltroBusqueda::normal).toList();
        return (capacidadMinima == null || apartamento.capacidad().admite(capacidadMinima))
                && caracteristicas.stream().map(FiltroBusqueda::normal).allMatch(dotacion::contains)
                && servicios.stream().map(FiltroBusqueda::normal).allMatch(activos::contains);
    }

    public boolean admitePrecio(BigDecimal valorEstimado) {
        return (precioMinimo == null || valorEstimado.compareTo(precioMinimo) >= 0)
                && (precioMaximo == null || valorEstimado.compareTo(precioMaximo) <= 0);
    }

    private static String normal(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }
}
