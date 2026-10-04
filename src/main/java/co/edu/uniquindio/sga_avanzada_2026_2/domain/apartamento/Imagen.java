package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: imagen del apartamento.
 */
public record Imagen(String url, boolean principal) {

    public Imagen {
        if (url == null || url.isBlank()) {
            throw new ReglaDominioException("La url de la imagen es obligatoria");
        }
    }
}
