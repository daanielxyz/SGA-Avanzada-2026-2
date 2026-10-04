package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: código de negocio de la reserva.
 */
public record ReservaId(String valor) {

    public ReservaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("ReservaId es obligatorio");
        }
        if (!valor.matches("RES-\\d{4}-\\d{5}")) {
            throw new ReglaDominioException("ReservaId con formato inválido (ej. RES-2026-00042): " + valor);
        }
    }
}
