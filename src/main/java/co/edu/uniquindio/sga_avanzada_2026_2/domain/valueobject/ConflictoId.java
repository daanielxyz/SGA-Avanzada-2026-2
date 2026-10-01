package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de ConflictoCanal.
 */
public record ConflictoId(UUID valor) {

    public ConflictoId {
        Objects.requireNonNull(valor, "El valor de ConflictoId no puede ser nulo");
    }

    public static ConflictoId nuevo() {
        return new ConflictoId(UUID.randomUUID());
    }

    public static ConflictoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new ConflictoId(UUID.fromString(uuidStr));
    }
}
