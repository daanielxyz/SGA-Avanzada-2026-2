package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Cargo.
 */
public record CargoId(UUID valor) {

    public CargoId {
        Objects.requireNonNull(valor, "El valor de CargoId no puede ser nulo");
    }

    public static CargoId nuevo() {
        return new CargoId(UUID.randomUUID());
    }

    public static CargoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new CargoId(UUID.fromString(uuidStr));
    }
}
