package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: cargo de un folio.
 */
public record CargoId(String valor) {

    public CargoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("CargoId es obligatorio");
        }
    }
}
