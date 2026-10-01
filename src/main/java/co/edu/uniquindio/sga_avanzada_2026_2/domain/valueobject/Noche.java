package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Objeto de valor: Noche individual dentro de un rango de estancia.
 */
public record Noche(LocalDate fecha) {

    public Noche {
        Objects.requireNonNull(fecha, "La fecha de la noche no puede ser nula");
    }
}
