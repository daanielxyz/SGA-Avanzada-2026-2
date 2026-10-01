package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Política de Cancelación.
 */
public record PoliticaId(UUID valor) {

    public PoliticaId {
        Objects.requireNonNull(valor, "El valor de PoliticaId no puede ser nulo");
    }

    public static PoliticaId nuevo() {
        return new PoliticaId(UUID.randomUUID());
    }

    public static PoliticaId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new PoliticaId(UUID.fromString(uuidStr));
    }
}
