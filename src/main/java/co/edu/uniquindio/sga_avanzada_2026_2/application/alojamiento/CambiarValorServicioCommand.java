package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import java.math.BigDecimal;

/**
 * Entrada de {@link CambiarValorServicio} (SERV-02).
 */
public record CambiarValorServicioCommand(String alojamientoId, String servicioId, BigDecimal valor) {
}
