package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;

/**
 * Objeto de valor: Imagen asociada a un apartamento.
 */
public record Imagen(String url, boolean principal) {

    public Imagen {
        Objects.requireNonNull(url, "La URL de la imagen no puede ser nula");
        if (url.isBlank()) {
            throw new IllegalArgumentException("La URL de la imagen no puede estar vacía");
        }
        url = url.trim();
    }
}
