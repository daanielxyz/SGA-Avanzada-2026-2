package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: temporada del calendario.
 */
public record TemporadaId(String valor) {

    public TemporadaId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("TemporadaId es obligatorio");
        }
    }
}
