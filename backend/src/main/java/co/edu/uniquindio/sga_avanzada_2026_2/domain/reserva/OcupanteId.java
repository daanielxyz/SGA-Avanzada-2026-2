package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: ocupante de una reserva.
 */
public record OcupanteId(String valor) {

    public OcupanteId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("OcupanteId es obligatorio");
        }
    }
}
