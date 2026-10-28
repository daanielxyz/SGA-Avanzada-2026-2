package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades {@code sga.cors.*} (DEC-81): orígenes de los frontends ({@code web/}, {@code pms/}) publicados en una
 * dirección distinta a la de la API. Llegan por {@code SGA_CORS_ORIGENES}, separados por comas; en local no hacen
 * falta, porque el proxy de Vite reenvía {@code /api} al backend.
 *
 * @param origenes orígenes exactos ({@code https://reservas.ejemplo.com}); vacío = CORS apagado
 */
@ConfigurationProperties("sga.cors")
public record CorsPropiedades(List<String> origenes) {

    public CorsPropiedades {
        origenes = origenes == null ? List.of() : origenes.stream()
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }
}
