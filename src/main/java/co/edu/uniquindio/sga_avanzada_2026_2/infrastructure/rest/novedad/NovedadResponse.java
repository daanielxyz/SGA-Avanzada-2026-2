package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.NovedadResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON de una novedad con el historial de sus pasos (DEC-31 · NOV-04).
 *
 * @param estado    ABIERTA, EN_REVISION o CERRADA: el del último paso
 * @param historial del registro al paso más reciente, cada uno con su autor y fecha
 */
public record NovedadResponse(String id, String apartamento, String descripcion, String gravedad, String estado,
                              List<CambioResponse> historial) {

    public record CambioResponse(String estado, String autor, LocalDateTime fechaHora) {
    }

    static NovedadResponse de(NovedadResult r) {
        return new NovedadResponse(r.id(), r.apartamento(), r.descripcion(), r.gravedad(), r.estado(),
                r.historial().stream().map(c -> new CambioResponse(c.estado(), c.autor(), c.fechaHora())).toList());
    }
}
