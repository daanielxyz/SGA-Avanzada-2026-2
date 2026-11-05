package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Entrada de {@link ReservarDesdePortalUseCase} (CU-10). A diferencia de recepción, la hora de llegada es obligatoria:
 * pagar confirma la reserva y confirmar la exige (RN-09 · DEC-69).
 *
 * @param salida    exclusiva: esa noche no se ocupa (EST-01)
 * @param vehiculos los que ocupan parqueadero (RP-02)
 * @param mascotas  las que lleva el grupo (RP-03)
 */
public record ReservarDesdePortalCommand(String apartamento, LocalDate entrada, LocalDate salida,
                                         TitularCommand titular, List<OcupanteCommand> ocupantes,
                                         LocalTime horaEstimadaLlegada, int vehiculos, int mascotas) {
}
