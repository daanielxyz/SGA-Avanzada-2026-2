package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: evento de la bitácora de canal.
 */
public record EventoCanalId(String valor) {

    public EventoCanalId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("EventoCanalId es obligatorio");
        }
    }
}
