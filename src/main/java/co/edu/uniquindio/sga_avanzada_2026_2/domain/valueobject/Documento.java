package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;

/**
 * Objeto de valor: Documento de identificación formal.
 */
public record Documento(TipoDocumento tipo, String numero) {

    public Documento {
        Objects.requireNonNull(tipo, "El tipo de documento no puede ser nulo");
        Objects.requireNonNull(numero, "El número de documento no puede ser nulo");
        if (numero.isBlank()) {
            throw new IllegalArgumentException("El número de documento no puede estar vacío");
        }
        numero = numero.trim();
    }
}
