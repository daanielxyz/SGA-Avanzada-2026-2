package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

/**
 * Entrada de {@link AvanzarNovedad} (CU-55).
 *
 * @param autor quien da el paso; en B3 sale del usuario autenticado (DEC-61)
 */
public record AvanzarNovedadCommand(String id, String autor) {
}
