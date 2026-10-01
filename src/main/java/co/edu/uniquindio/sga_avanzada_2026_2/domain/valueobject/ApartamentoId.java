package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Apartamento.
 */
public record ApartamentoId(UUID valor) {

    public ApartamentoId {
        Objects.requireNonNull(valor, "El valor de ApartamentoId no puede ser nulo");
    }

    public static ApartamentoId nuevo() {
        return new ApartamentoId(UUID.randomUUID());
    }

    public static ApartamentoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new ApartamentoId(UUID.fromString(uuidStr));
    }
}
