package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ModificarReservaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/modificar} (CU-17): los datos completos como quedan, no solo lo que
 * cambia (DEC-38).
 *
 * @param apartamento el mismo u otro apartamento destino
 * @param salida      exclusiva (EST-01)
 * @param ocupantes   el grupo completo; el titular sigue siendo el mismo (TIT-01)
 * @param carros      los que de verdad llegan; si no se envía, se conservan los de la reserva (BL-23)
 * @param motos       ídem
 * @param mascotas    ídem
 */
public record ModificarReservaRequest(
        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotEmpty(message = "debe tener al menos un ocupante")
        List<@Valid @NotNull(message = "no puede ser nulo") OcupanteRequest> ocupantes,

        // Opcionales (BL-23): sin ellos, la reserva conserva sus vehículos y mascotas
        @Min(value = 0, message = "no puede ser negativo")
        Integer carros,

        @Min(value = 0, message = "no puede ser negativo")
        Integer motos,

        @Min(value = 0, message = "no puede ser negativo")
        Integer mascotas) {

    ModificarReservaCommand aCommand(String codigo) {
        return new ModificarReservaCommand(codigo, apartamento, entrada, salida,
                OcupanteRequest.aCommands(ocupantes), carros, motos, mascotas);
    }
}
