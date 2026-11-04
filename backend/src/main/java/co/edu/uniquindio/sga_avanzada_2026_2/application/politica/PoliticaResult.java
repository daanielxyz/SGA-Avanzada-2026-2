package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Salida de los casos de uso de la política de cancelación (DEC-31): la versión que acepta el huésped al reservar.
 */
public record PoliticaResult(String id, String alojamientoId, int version, List<TramoResult> tramos,
                             PenalizacionResult penalizacionNoShow) {

    public record TramoResult(int antelacionMinHoras, PenalizacionResult penalizacion) {
    }

    /** Solo uno de {@code porcentaje} y {@code montoFijo} viene con valor. */
    public record PenalizacionResult(String base, Integer porcentaje, BigDecimal montoFijo) {

        static PenalizacionResult de(Penalizacion p) {
            return new PenalizacionResult(p.base().name(),
                    Optional.ofNullable(p.porcentaje()).map(Porcentaje::valor).orElse(null),
                    Optional.ofNullable(p.montoFijo()).map(Dinero::monto).orElse(null));
        }
    }

    public static PoliticaResult de(PoliticaCancelacion politica) {
        return new PoliticaResult(
                politica.id().valor(),
                politica.alojamientoId().valor(),
                politica.version(),
                politica.tramos().stream()
                        .map(t -> new TramoResult(t.antelacionMinHoras(), PenalizacionResult.de(t.penalizacion())))
                        .toList(),
                PenalizacionResult.de(politica.penalizacionNoShow()));
    }
}
