package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoEncontradoResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON de la tarjeta de un apartamento libre (CU-07 · sección 7.5).
 */
public record ApartamentoEncontradoResponse(String codigo, String nombre, int capacidad, int dormitorios,
                                            String imagenPrincipal, List<String> caracteristicas,
                                            BigDecimal valorEstimado, String ciudad, double latitud, double longitud) {

    static ApartamentoEncontradoResponse de(ApartamentoEncontradoResult r) {
        return new ApartamentoEncontradoResponse(r.codigo(), r.nombre(), r.capacidad(), r.dormitorios(),
                r.imagenPrincipal(), r.caracteristicas(), r.valorEstimado(), r.ciudad(), r.latitud(), r.longitud());
    }
}
