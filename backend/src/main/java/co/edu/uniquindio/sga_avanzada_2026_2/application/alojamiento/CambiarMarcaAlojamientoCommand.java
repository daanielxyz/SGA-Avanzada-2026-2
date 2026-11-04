package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

/**
 * Entrada de {@link CambiarMarcaAlojamientoUseCase} (BL-05).
 */
public record CambiarMarcaAlojamientoCommand(String alojamientoId, MarcaDatos marca) {
}
