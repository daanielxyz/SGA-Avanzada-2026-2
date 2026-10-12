package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link RecibirReservaExterna} (CU-12): lo que pidió el canal, ya autenticado y traducido por su
 * adaptador (B4).
 *
 * @param idExterno identificador de la reserva en el canal; con el canal da la idempotencia (RN-19)
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param ocupantes el grupo completo, incluido el titular con su documento (TIT-01)
 * @param cargaUtil el mensaje original sin credenciales, para la bitácora (BIT-04)
 */
public record RecibirReservaExternaCommand(String canalId, String idExterno, String apartamento, LocalDate entrada,
                                           LocalDate salida, TitularCommand titular,
                                           List<OcupanteCommand> ocupantes, String cargaUtil) {
}
