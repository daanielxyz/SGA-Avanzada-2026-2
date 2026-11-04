package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: pago de un folio.
 */
public record PagoId(String valor) {

    public PagoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("PagoId es obligatorio");
        }
    }
}
