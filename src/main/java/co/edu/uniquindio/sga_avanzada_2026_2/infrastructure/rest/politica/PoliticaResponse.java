package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PoliticaResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON de una versión de la política de cancelación (DEC-31): la que acepta el huésped al reservar.
 */
public record PoliticaResponse(String id, int version, List<TramoResponse> tramos,
                               PenalizacionResponse penalizacionNoShow) {

    public record TramoResponse(int antelacionMinHoras, PenalizacionResponse penalizacion) {
    }

    /**
     * @param porcentaje vacío si la penalización es un monto fijo
     * @param montoFijo  vacío si la penalización es un porcentaje
     */
    public record PenalizacionResponse(String base, Integer porcentaje, BigDecimal montoFijo) {

        static PenalizacionResponse de(PoliticaResult.PenalizacionResult p) {
            return new PenalizacionResponse(p.base(), p.porcentaje(), p.montoFijo());
        }
    }

    static PoliticaResponse de(PoliticaResult r) {
        return new PoliticaResponse(r.id(), r.version(),
                r.tramos().stream()
                        .map(t -> new TramoResponse(t.antelacionMinHoras(), PenalizacionResponse.de(t.penalizacion())))
                        .toList(),
                PenalizacionResponse.de(r.penalizacionNoShow()));
    }
}
