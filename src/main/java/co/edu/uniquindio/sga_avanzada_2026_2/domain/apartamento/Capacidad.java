package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: número máximo de ocupantes (≥1).
 */
public record Capacidad(int valor) {

    public Capacidad {
        if (valor < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos 1");
        }
    }
}
