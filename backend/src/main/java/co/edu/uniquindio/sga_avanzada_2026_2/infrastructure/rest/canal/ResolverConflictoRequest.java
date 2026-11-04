package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ResolverConflictoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/conflictos/{id}/resolver} (CONF-04).
 *
 * @param decision qué se hizo con la reserva rechazada, en texto libre
 */
public record ResolverConflictoRequest(
        @NotBlank(message = "es obligatoria")
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String decision) {

    ResolverConflictoCommand aCommand(String id, String autor) {
        return new ResolverConflictoCommand(id, decision, autor);
    }
}
