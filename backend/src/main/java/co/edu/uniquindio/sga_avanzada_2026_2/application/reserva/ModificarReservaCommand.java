package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link ModificarReservaUseCase} (CU-17): los datos completos como quedan, no solo lo que cambia (DEC-38).
 *
 * @param apartamento el mismo u otro apartamento destino
 * @param ocupantes   el grupo completo; el titular sigue siendo el mismo (TIT-01)
 * @param carros      los que de verdad llegan; nulo conserva los de la reserva (BL-23)
 * @param motos       ídem
 * @param mascotas    ídem
 */
public record ModificarReservaCommand(String codigo, String apartamento, LocalDate entrada, LocalDate salida,
                                      List<OcupanteCommand> ocupantes, Integer carros, Integer motos,
                                      Integer mascotas) {

    /** Sin tocar vehículos ni mascotas. */
    public ModificarReservaCommand(String codigo, String apartamento, LocalDate entrada, LocalDate salida,
                                   List<OcupanteCommand> ocupantes) {
        this(codigo, apartamento, entrada, salida, ocupantes, null, null, null);
    }
}
