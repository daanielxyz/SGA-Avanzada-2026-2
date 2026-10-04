package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: alojamiento (inquilino del SaaS).
 */
public record AlojamientoId(String valor) {

    public AlojamientoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("AlojamientoId es obligatorio");
        }
    }
}
