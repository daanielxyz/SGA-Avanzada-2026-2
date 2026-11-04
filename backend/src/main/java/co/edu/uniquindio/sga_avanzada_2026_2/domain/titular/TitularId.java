package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: titular de reservas.
 */
public record TitularId(String valor) {

    public TitularId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("TitularId es obligatorio");
        }
    }
}
