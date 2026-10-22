package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

/**
 * Entrada de {@link RegistrarCanalUseCase} (CU-54).
 *
 * @param tipo                 nombre de {@code CanalOrigen}: PORTAL, DIRECTO o EXTERNO
 * @param referenciaCredencial solo para EXTERNO: nombre del secreto en la configuración externa (DEC-47)
 */
public record RegistrarCanalCommand(String alojamientoId, String nombre, String tipo, String referenciaCredencial) {
}
