package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: servicio adicional del alojamiento.
 */
public record ServicioAdicionalId(String valor) {

    public ServicioAdicionalId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("ServicioAdicionalId es obligatorio");
        }
    }
}
