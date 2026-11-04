package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RegistrarBloqueoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /api/apartamentos/{codigo}/bloqueos} (CU-32).
 */
public record RegistrarBloqueoRequest(
        @NotNull(message = "es obligatorio")
        LocalDate inicio,

        @NotNull(message = "es obligatorio")
        LocalDate fin,

        @NotBlank(message = "es obligatorio")
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivo) {

    RegistrarBloqueoCommand aCommand(String codigo) {
        return new RegistrarBloqueoCommand(codigo, inicio, fin, motivo);
    }
}
