package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: versión de una tarifa.
 */
public record TarifaId(String valor) {

    public TarifaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("TarifaId es obligatorio");
        }
    }
}
