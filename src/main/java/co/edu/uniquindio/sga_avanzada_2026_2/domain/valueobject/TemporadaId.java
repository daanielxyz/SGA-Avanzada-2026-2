package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Temporada.
 */
public record TemporadaId(UUID valor) {

    public TemporadaId {
        Objects.requireNonNull(valor, "El valor de TemporadaId no puede ser nulo");
    }

    public static TemporadaId nuevo() {
        return new TemporadaId(UUID.randomUUID());
    }

    public static TemporadaId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new TemporadaId(UUID.fromString(uuidStr));
    }
}
