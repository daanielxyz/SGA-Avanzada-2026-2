package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: bloqueo de un apartamento.
 */
public record BloqueoId(String valor) {

    public BloqueoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("BloqueoId es obligatorio");
        }
    }
}
