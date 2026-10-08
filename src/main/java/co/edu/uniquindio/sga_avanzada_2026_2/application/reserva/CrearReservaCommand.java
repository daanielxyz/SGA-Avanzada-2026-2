package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Entrada de {@link CrearReserva} (CU-10 · CU-11). Las reservas EXTERNO llegan por {@code RecibirReservaExterna}
 * (B1d).
 *
 * @param salida              exclusiva: esa noche no se ocupa (EST-01)
 * @param canalOrigen         PORTAL o DIRECTO
 * @param ocupantes           el grupo completo, incluido el titular con su documento (TIT-01)
 * @param horaEstimadaLlegada opcional al crear; necesaria para confirmar (RN-09)
 */
public record CrearReservaCommand(String apartamento, LocalDate entrada, LocalDate salida, String canalOrigen,
                                  TitularCommand titular, List<OcupanteCommand> ocupantes,
                                  LocalTime horaEstimadaLlegada) {
}
