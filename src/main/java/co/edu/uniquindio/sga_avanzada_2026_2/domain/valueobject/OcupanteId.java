package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Ocupante.
 */
public record OcupanteId(UUID valor) {

    public OcupanteId {
        Objects.requireNonNull(valor, "El valor de OcupanteId no puede ser nulo");
    }

    public static OcupanteId nuevo() {
        return new OcupanteId(UUID.randomUUID());
    }

    public static OcupanteId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new OcupanteId(UUID.fromString(uuidStr));
    }
}
