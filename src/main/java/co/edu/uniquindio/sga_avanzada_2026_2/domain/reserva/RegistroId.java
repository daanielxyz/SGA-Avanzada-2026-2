package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: registro (check-in) de una reserva.
 */
public record RegistroId(String valor) {

    public RegistroId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("RegistroId es obligatorio");
        }
    }
}
