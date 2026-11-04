package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tarjeta de un apartamento libre (CU-07 · sección 7.5): imagen principal, capacidad, valor estimado y ubicación.
 */
public record ApartamentoEncontradoResult(String codigo, String nombre, int capacidad, int dormitorios,
                                          String imagenPrincipal, List<String> caracteristicas,
                                          BigDecimal valorEstimado, String ciudad, double latitud, double longitud) {
}
