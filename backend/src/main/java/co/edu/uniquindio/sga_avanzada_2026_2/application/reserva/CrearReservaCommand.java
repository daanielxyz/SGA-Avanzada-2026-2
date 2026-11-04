package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Entrada de {@link CrearReservaUseCase} (CU-10 · CU-11). Las reservas EXTERNO llegan por
 * {@code RecibirReservaExternaUseCase} (DEC-62).
 *
 * @param salida              exclusiva: esa noche no se ocupa (EST-01)
 * @param canalOrigen         PORTAL o DIRECTO
 * @param ocupantes           el grupo completo, incluido el titular con su documento (TIT-01)
 * @param horaEstimadaLlegada opcional al crear; necesaria para confirmar (RN-09)
 * @param vehiculos           los que ocupan parqueadero (RP-02)
 * @param mascotas            las que lleva el grupo (RP-03)
 */
public record CrearReservaCommand(String apartamento, LocalDate entrada, LocalDate salida, String canalOrigen,
                                  TitularCommand titular, List<OcupanteCommand> ocupantes,
                                  LocalTime horaEstimadaLlegada, int vehiculos, int mascotas) {

    /** Sin vehículos ni mascotas, como reserva el portal. */
    public CrearReservaCommand(String apartamento, LocalDate entrada, LocalDate salida, String canalOrigen,
                               TitularCommand titular, List<OcupanteCommand> ocupantes,
                               LocalTime horaEstimadaLlegada) {
        this(apartamento, entrada, salida, canalOrigen, titular, ocupantes, horaEstimadaLlegada, 0, 0);
    }
}
