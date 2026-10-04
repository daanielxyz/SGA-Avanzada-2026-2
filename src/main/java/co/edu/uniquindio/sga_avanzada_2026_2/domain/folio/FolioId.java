package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: folio de una reserva.
 */
public record FolioId(String valor) {

    public FolioId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("FolioId es obligatorio");
        }
    }
}
