package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: novedad de un apartamento.
 */
public record NovedadId(String valor) {

    public NovedadId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("NovedadId es obligatorio");
        }
    }
}
