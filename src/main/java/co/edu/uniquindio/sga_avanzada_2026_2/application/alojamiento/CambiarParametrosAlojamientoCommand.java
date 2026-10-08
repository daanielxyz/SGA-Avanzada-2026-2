package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

/**
 * Entrada de {@link CambiarParametrosAlojamiento} (CU-30): la configuración completa que la reemplaza.
 */
public record CambiarParametrosAlojamientoCommand(String alojamientoId, ParametrosCommand parametros) {
}
