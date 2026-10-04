package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: salida (check-out) de una reserva.
 */
public record SalidaId(String valor) {

    public SalidaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("SalidaId es obligatorio");
        }
    }
}
