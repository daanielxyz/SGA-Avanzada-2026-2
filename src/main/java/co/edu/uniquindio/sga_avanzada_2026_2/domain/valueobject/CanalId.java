package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Canal.
 */
public record CanalId(UUID valor) {

    public CanalId {
        Objects.requireNonNull(valor, "El valor de CanalId no puede ser nulo");
    }

    public static CanalId nuevo() {
        return new CanalId(UUID.randomUUID());
    }

    public static CanalId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new CanalId(UUID.fromString(uuidStr));
    }
}
