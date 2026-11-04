package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * VO: imagen del apartamento. Solo se guarda su URL; el binario vive en un servicio externo (IMG-04).
 */
public record Imagen(String url, boolean principal) {

    // IMG-04
    public Imagen {
        if (url == null || url.isBlank()) {
            throw new ReglaDominioException("La url de la imagen es obligatoria");
        }
        url = url.trim();
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                throw new ReglaDominioException("La url de la imagen debe ser http(s) válida: " + url);
            }
        } catch (URISyntaxException e) {
            throw new ReglaDominioException("La url de la imagen no es válida: " + url);
        }
    }
}
