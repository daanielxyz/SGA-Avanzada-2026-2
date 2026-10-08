package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import java.util.List;

/**
 * Salida de {@link CambiarCapacidad}: el apartamento y la advertencia de APA-15.
 *
 * @param reservasAfectadas códigos de las reservas activas cuyo grupo ya no cabe; no se modifican
 */
public record CambioCapacidadResult(ApartamentoResult apartamento, List<String> reservasAfectadas) {
}
