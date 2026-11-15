package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
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

    /**
     * @param autorNombre su nombre vigente, solo con {@link NovedadResult#conNombres}; nulo si esa cuenta ya no
     *                    existe (DEC-132, como DEC-103)
     */
    public record CambioResult(String estado, String autor, String autorNombre, LocalDateTime fechaHora) {
        static CambioResult de(CambioEstadoNovedad cambio) {
            return new CambioResult(cambio.estado().name(), cambio.autor().valor(), null, cambio.fechaHora());
        }
    }

    public static NovedadResult de(Novedad novedad) {
        return new NovedadResult(novedad.id().valor(), novedad.apartamentoId().valor(), novedad.descripcion(),
                novedad.gravedad().name(), novedad.estado().name(),
                novedad.historial().stream().map(CambioResult::de).toList());
    }

    /** La misma novedad con el nombre de quien dio cada paso: para el personal, que ve quién reportó y quién cerró. */
    public NovedadResult conNombres(DirectorioPersonal personal) {
        return new NovedadResult(id, apartamento, descripcion, gravedad, estado, historial.stream()
                .map(c -> new CambioResult(c.estado(), c.autor(),
                        personal.nombreDe(new UsuarioId(c.autor())).orElse(null), c.fechaHora()))
                .toList());
    }
}
