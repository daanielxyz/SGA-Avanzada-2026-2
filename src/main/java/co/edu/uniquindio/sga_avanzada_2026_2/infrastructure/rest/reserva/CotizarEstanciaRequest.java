package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarEstanciaCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/cotizaciones} (CU-09): para cotizar basta la fecha de nacimiento de cada ocupante
 * (RN-06).
 *
 * @param salida exclusiva (EST-01)
 */
public record CotizarEstanciaRequest(
        @NotBlank(message = "es obligatorio")
        String apartamento,

        @NotNull(message = "es obligatoria")
        LocalDate entrada,

        @NotNull(message = "es obligatoria")
        LocalDate salida,

        @NotEmpty(message = "debe tener al menos una fecha de nacimiento")
        List<@NotNull(message = "no puede ser nula") LocalDate> fechasNacimiento) {

    CotizarEstanciaCommand aCommand() {
        return new CotizarEstanciaCommand(apartamento, entrada, salida, fechasNacimiento);
    }
}
