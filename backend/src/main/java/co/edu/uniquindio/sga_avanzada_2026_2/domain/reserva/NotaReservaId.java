package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: nota de una reserva para el siguiente turno ({@code NOT-12}, BL-08).
 */
public record NotaReservaId(String valor) {

    public NotaReservaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("NotaReservaId es obligatorio");
        }
    }
}
