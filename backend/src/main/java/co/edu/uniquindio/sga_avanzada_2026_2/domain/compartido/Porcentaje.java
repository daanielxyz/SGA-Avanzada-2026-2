package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * VO: porcentaje entero entre 0 y 100 inclusive (PORC-01).
 */
public record Porcentaje(int valor) {

    // PORC-01
    public Porcentaje {
        if (valor < 0 || valor > 100) {
            throw new ReglaDominioException("El porcentaje debe estar entre 0 y 100: " + valor);
        }
    }
}
