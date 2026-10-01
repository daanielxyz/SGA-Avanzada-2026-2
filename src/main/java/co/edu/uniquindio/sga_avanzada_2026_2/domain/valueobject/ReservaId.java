package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Reserva.
 */
public record ReservaId(UUID valor) {

    public ReservaId {
        Objects.requireNonNull(valor, "El valor de ReservaId no puede ser nulo");
    }

    public static ReservaId nuevo() {
        return new ReservaId(UUID.randomUUID());
    }

    public static ReservaId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new ReservaId(UUID.fromString(uuidStr));
    }
}
