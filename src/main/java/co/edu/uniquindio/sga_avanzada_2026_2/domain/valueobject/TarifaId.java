package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Tarifa.
 */
public record TarifaId(UUID valor) {

    public TarifaId {
        Objects.requireNonNull(valor, "El valor de TarifaId no puede ser nulo");
    }

    public static TarifaId nuevo() {
        return new TarifaId(UUID.randomUUID());
    }

    public static TarifaId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new TarifaId(UUID.fromString(uuidStr));
    }
}
