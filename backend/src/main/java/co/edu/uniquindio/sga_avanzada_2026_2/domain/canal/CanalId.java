package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: canal de venta.
 */
public record CanalId(String valor) {

    public CanalId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("CanalId es obligatorio");
        }
    }
}
