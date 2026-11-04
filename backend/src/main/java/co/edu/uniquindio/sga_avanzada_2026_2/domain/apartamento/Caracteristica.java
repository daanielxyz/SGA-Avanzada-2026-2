package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: etiqueta de dotación del apartamento, p. ej. balcón o cocina equipada (CARAC-01).
 */
public record Caracteristica(String nombre) {

    // CARAC-01
    public Caracteristica {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre de la característica es obligatorio");
        }
        nombre = nombre.trim();
    }
}
