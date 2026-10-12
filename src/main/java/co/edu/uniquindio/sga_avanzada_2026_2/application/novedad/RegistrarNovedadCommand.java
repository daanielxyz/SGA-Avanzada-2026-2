package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

/**
 * Entrada de {@link RegistrarNovedad} (CU-43).
 *
 * @param gravedad nombre de {@code GravedadNovedad}
 * @param autor    quien la reporta, también el personal de servicio (NOV-05); en B3 sale del usuario autenticado
 *                 (DEC-61)
 */
public record RegistrarNovedadCommand(String apartamento, String descripcion, String gravedad, String autor) {
}
