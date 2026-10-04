package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.time.LocalDate;

/**
 * VO: intervalo [entrada, salida); la noche de salida no se ocupa ni se cobra.
 */
public record Estancia(LocalDate entrada, LocalDate salida) {

    // RN-03
    public Estancia {
        if (entrada == null || salida == null) {
            throw new ReglaDominioException("Entrada y salida son obligatorias");
        }
        if (!salida.isAfter(entrada)) {
            throw new ReglaDominioException("La salida debe ser posterior a la entrada");
        }
    }
}
