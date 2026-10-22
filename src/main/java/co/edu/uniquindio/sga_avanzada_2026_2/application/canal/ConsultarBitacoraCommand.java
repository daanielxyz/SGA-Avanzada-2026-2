package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import java.time.LocalDate;

/**
 * Entrada de {@link ConsultarBitacoraUseCase} (CU-45).
 *
 * @param desde        primer día, incluido
 * @param hasta        último día, incluido
 * @param numeroPagina empieza en 0
 */
public record ConsultarBitacoraCommand(String canalId, LocalDate desde, LocalDate hasta, int numeroPagina) {
}
