package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Alojamiento.
 */
public record AlojamientoId(UUID valor) {

    public AlojamientoId {
        Objects.requireNonNull(valor, "El valor de AlojamientoId no puede ser nulo");
    }

    public static AlojamientoId nuevo() {
        return new AlojamientoId(UUID.randomUUID());
    }

    public static AlojamientoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new AlojamientoId(UUID.fromString(uuidStr));
    }
}
