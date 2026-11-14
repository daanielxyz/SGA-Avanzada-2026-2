package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirDocumentoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirDocumentoTitularCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/reservas/{codigo}/ocupantes/{ocupanteId}/documento} y de
 * {@code PUT /api/reservas/{codigo}/titular/documento} (#14 · DEC-120): el documento como debe quedar y por qué se
 * corrige. El autor sale de la sesión (DEC-61).
 */
public record CorregirDocumentoRequest(
        @NotNull(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI|RC", message = "debe ser CC, CE, PASAPORTE, TI o RC")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivo) {

    CorregirDocumentoCommand aCommand(String codigo, String ocupanteId, String autor) {
        return new CorregirDocumentoCommand(codigo, ocupanteId, tipoDocumento, numeroDocumento, motivo, autor);
    }

    CorregirDocumentoTitularCommand aCommandDelTitular(String codigo, String autor) {
        return new CorregirDocumentoTitularCommand(codigo, tipoDocumento, numeroDocumento, motivo, autor);
    }
}
