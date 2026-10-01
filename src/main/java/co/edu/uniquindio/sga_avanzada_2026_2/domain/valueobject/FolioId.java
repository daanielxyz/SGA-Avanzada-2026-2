package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Folio.
 */
public record FolioId(UUID valor) {

    public FolioId {
        Objects.requireNonNull(valor, "El valor de FolioId no puede ser nulo");
    }

    public static FolioId nuevo() {
        return new FolioId(UUID.randomUUID());
    }

    public static FolioId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new FolioId(UUID.fromString(uuidStr));
    }
}
