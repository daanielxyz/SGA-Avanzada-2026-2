package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

/**
 * Entrada de {@link CambiarEstadoOperativo} (CU-40 · CU-42).
 *
 * @param estado nombre de {@code EstadoOperativo}
 */
public record CambiarEstadoOperativoCommand(String codigo, String estado) {
}
