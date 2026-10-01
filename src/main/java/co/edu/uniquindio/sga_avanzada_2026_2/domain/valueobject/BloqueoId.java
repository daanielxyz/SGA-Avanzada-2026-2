package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Bloqueo.
 */
public record BloqueoId(UUID valor) {

    public BloqueoId {
        Objects.requireNonNull(valor, "El valor de BloqueoId no puede ser nulo");
    }

    public static BloqueoId nuevo() {
        return new BloqueoId(UUID.randomUUID());
    }

    public static BloqueoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new BloqueoId(UUID.fromString(uuidStr));
    }
}
