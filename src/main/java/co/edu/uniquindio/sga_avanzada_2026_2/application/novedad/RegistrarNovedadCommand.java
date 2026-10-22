package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

/**
 * Entrada de {@link RegistrarNovedadUseCase} (CU-43).
 *
 * @param gravedad nombre de {@code GravedadNovedad}
 * @param autor    quien la reporta, también el personal de servicio (NOV-05); es el usuario autenticado
 *                 (DEC-61 · DEC-67)
 */
public record RegistrarNovedadCommand(String apartamento, String descripcion, String gravedad, String autor) {
}
