package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

/**
 * Entrada de {@link AvanzarNovedadUseCase} (CU-55).
 *
 * @param autor quien da el paso; es el usuario autenticado (DEC-61 · DEC-67)
 */
public record AvanzarNovedadCommand(String id, String autor) {
}
