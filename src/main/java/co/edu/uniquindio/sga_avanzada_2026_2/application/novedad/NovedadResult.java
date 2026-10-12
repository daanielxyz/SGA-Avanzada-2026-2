package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.CambioEstadoNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida de los casos de uso de Novedad (DEC-31): la novedad con el historial de sus pasos (NOV-04).
 *
 * @param estado    ABIERTA, EN_REVISION o CERRADA: el del último paso
 * @param historial del registro al paso más reciente, cada uno con su autor y fecha
 */
public record NovedadResult(String id, String apartamento, String descripcion, String gravedad, String estado,
                            List<CambioResult> historial) {

    public record CambioResult(String estado, String autor, LocalDateTime fechaHora) {

        static CambioResult de(CambioEstadoNovedad cambio) {
            return new CambioResult(cambio.estado().name(), cambio.autor().valor(), cambio.fechaHora());
        }
    }

    public static NovedadResult de(Novedad novedad) {
        return new NovedadResult(novedad.id().valor(), novedad.apartamentoId().valor(), novedad.descripcion(),
                novedad.gravedad().name(), novedad.estado().name(),
                novedad.historial().stream().map(CambioResult::de).toList());
    }
}
