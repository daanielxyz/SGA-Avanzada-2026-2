package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Objeto de valor: Autorización para el cierre formal de un Folio.
 */
public record AutorizacionCierre(UsuarioId autor, String motivo, LocalDateTime fechaHora) {

    public AutorizacionCierre {
        Objects.requireNonNull(autor, "El autor de la autorización no puede ser nulo");
        Objects.requireNonNull(motivo, "El motivo de la autorización no puede ser nulo");
        Objects.requireNonNull(fechaHora, "La fecha y hora de la autorización no pueden ser nulas");
        if (motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la autorización no puede estar en blanco");
        }
        motivo = motivo.trim();
    }
}
