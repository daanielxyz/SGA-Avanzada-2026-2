package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import java.time.LocalDate;

/**
 * Entrada de {@link ConsultarDisponibilidadCanal} (CU-13).
 *
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes cuántas personas trae el grupo
 * @param cargaUtil la consulta como llegó, para la bitácora (BIT-04)
 */
public record ConsultarDisponibilidadCanalCommand(String canalId, LocalDate entrada, LocalDate salida, int ocupantes,
                                                  String cargaUtil) {
}
