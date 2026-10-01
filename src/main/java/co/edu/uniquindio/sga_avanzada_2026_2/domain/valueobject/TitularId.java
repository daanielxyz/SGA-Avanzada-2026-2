package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Titular.
 */
public record TitularId(UUID valor) {

    public TitularId {
        Objects.requireNonNull(valor, "El valor de TitularId no puede ser nulo");
    }

    public static TitularId nuevo() {
        return new TitularId(UUID.randomUUID());
    }

    public static TitularId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new TitularId(UUID.fromString(uuidStr));
    }
}
