package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de valor: Identificador único de Pago.
 */
public record PagoId(UUID valor) {

    public PagoId {
        Objects.requireNonNull(valor, "El valor de PagoId no puede ser nulo");
    }

    public static PagoId nuevo() {
        return new PagoId(UUID.randomUUID());
    }

    public static PagoId de(String uuidStr) {
        Objects.requireNonNull(uuidStr, "El identificador no puede ser nulo");
        return new PagoId(UUID.fromString(uuidStr));
    }
}
