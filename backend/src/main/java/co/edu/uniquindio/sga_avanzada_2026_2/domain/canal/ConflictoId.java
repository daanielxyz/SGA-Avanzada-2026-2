package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: conflicto de canal externo.
 */
public record ConflictoId(String valor) {

    public ConflictoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("ConflictoId es obligatorio");
        }
    }
}
