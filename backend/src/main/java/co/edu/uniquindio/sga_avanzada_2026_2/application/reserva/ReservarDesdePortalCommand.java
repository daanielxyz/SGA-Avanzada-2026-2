package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link ReservarDesdePortalUseCase} (CU-10). No trae hora de llegada: es la de entrada del hotel (RN-09).
 *
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param carros    los que ocupan parqueadero (RP-02)
 * @param motos     las que ocupan parqueadero (RP-02)
 * @param mascotas  las que lleva el grupo (RP-03)
 */
public record ReservarDesdePortalCommand(String apartamento, LocalDate entrada, LocalDate salida,
                                         TitularCommand titular, List<OcupanteCommand> ocupantes,
                                         int carros, int motos, int mascotas) {
}
