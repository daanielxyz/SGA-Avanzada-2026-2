package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirRegistroCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/corregir-registro} (REG-06).
 *
 * @param fechaHora cuándo llegó realmente el grupo
 */
public record CorregirRegistroRequest(
        @NotNull(message = "es obligatoria")
        LocalDateTime fechaHora,

        @NotBlank(message = "es obligatorio")
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivo) {

    CorregirRegistroCommand aCommand(String codigo, String autor) {
        return new CorregirRegistroCommand(codigo, fechaHora, motivo, autor);
    }
}
