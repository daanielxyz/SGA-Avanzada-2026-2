package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.time.LocalDate;

/**
 * VO: unidad mínima de venta, identificada por la fecha en que se duerme (NOC-01). Dos noches con la misma
 * fecha son iguales. Inmutable (NOC-05). Bloqueo y Temporada comparan por Noche.
 */
public record Noche(LocalDate fecha) {

    // NOC-01
    public Noche {
        if (fecha == null) {
            throw new ReglaDominioException("La fecha de la noche es obligatoria");
        }
    }
}
