package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: versión de una política de cancelación.
 */
public record PoliticaId(String valor) {

    public PoliticaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("PoliticaId es obligatorio");
        }
    }
}
