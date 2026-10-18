package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

/**
 * Entrada de {@link CancelarReservaExterna} (CU-16).
 *
 * @param idExterno el identificador con el que el canal envió la reserva (RN-19)
 * @param cargaUtil la cancelación como llegó, para la bitácora (BIT-04)
 */
public record CancelarReservaExternaCommand(String canalId, String idExterno, String cargaUtil) {
}
