package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.time.LocalDate;

/**
 * VO: unidad de venta. Bloqueo y Temporada comparan por Noche.
 */
public record Noche(LocalDate fecha) {

    public Noche {
        if (fecha == null) {
            throw new ReglaDominioException("La fecha de la noche es obligatoria");
        }
    }
}
