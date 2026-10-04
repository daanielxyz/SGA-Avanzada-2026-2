package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: característica del apartamento.
 */
public record Caracteristica(String nombre) {

    public Caracteristica {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la característica es obligatorio");
        }
    }
}
