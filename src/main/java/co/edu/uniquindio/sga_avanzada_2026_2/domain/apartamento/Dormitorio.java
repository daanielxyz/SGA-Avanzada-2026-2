package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: cantidad de dormitorios (≥1).
 */
public record Dormitorio(int cantidad) {

    public Dormitorio {
        if (cantidad < 1) {
            throw new ReglaDominioException("Debe haber al menos un dormitorio");
        }
    }
}
