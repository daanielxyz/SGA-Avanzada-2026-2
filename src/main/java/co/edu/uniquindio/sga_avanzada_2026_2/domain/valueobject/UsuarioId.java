package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Usuario.
 */
public record UsuarioId(UUID valor) {

    public UsuarioId {
        Objects.requireNonNull(valor, "El valor de UsuarioId no puede ser nulo");
    }

    public static UsuarioId nuevo() {
        return new UsuarioId(UUID.randomUUID());
    }

    public static UsuarioId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new UsuarioId(UUID.fromString(uuidStr));
    }
}
