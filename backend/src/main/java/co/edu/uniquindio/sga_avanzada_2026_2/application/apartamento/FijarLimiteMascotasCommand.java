package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

/**
 * Entrada de {@link FijarLimiteMascotasUseCase}.
 *
 * @param limiteMascotas vacío para volver al límite del alojamiento
 */
public record FijarLimiteMascotasCommand(String codigo, Integer limiteMascotas) {
}
