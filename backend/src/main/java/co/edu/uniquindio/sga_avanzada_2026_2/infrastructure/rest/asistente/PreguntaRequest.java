package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.asistente.ResponderPreguntaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Una pregunta al asistente del PMS (DEC-127). Quién pregunta y su rol salen de la sesión, nunca de aquí.
 *
 * @param pantalla  la ruta del PMS donde está la persona, p. ej. {@code /reservas/RES-2026-00042/llegada}
 * @param historial los mensajes anteriores de la conversación, del más antiguo al más reciente
 */
public record PreguntaRequest(
        @NotBlank(message = "es obligatoria") @Size(max = 1000, message = "admite hasta 1000 caracteres")
        String pregunta,
        @Size(max = 200, message = "admite hasta 200 caracteres") String pantalla,
        @Size(max = 20, message = "admite hasta 20 mensajes") List<@Valid MensajeRequest> historial) {

    /**
     * @param autor PERSONA o ASISTENTE
     */
    public record MensajeRequest(
            @NotBlank(message = "es obligatorio")
            @Pattern(regexp = "PERSONA|ASISTENTE", message = "debe ser PERSONA o ASISTENTE") String autor,
            @NotBlank(message = "es obligatorio") @Size(max = 4000, message = "admite hasta 4000 caracteres")
            String texto) {
    }

    ResponderPreguntaCommand aCommand(String alojamientoId, String solicitante, String rol) {
        List<ResponderPreguntaCommand.Mensaje> mensajes = historial == null ? List.of() : historial.stream()
                .map(m -> new ResponderPreguntaCommand.Mensaje(m.autor(), m.texto()))
                .toList();
        return new ResponderPreguntaCommand(alojamientoId, solicitante, rol, pregunta, pantalla, mensajes);
    }
}
