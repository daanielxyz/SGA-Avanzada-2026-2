package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

/**
 * Entrada de {@link CambiarUbicacionAlojamiento} (CU-30): la ubicación completa que reemplaza la actual (UBI-03).
 */
public record CambiarUbicacionAlojamientoCommand(String alojamientoId, double latitud, double longitud) {
}
