package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PoliticaResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * JSON de la política de cancelación vigente para el huésped: las condiciones que acepta al reservar (DEC-41).
 *
 * @param tramos            de cada antelación mínima, en horas antes de la llegada, cuánto se retiene si cancela
 * @param penalizacionNoShow cuánto se retiene si no llega
 */
public record PoliticaPortalResponse(List<TramoResponse> tramos, PenalizacionResponse penalizacionNoShow) {

    public record TramoResponse(int antelacionMinHoras, PenalizacionResponse penalizacion) {
    }

    /**
     * @param base       sobre qué se calcula: {@code VALOR_TOTAL}, {@code PAGADO} o {@code ANTICIPO_EXIGIDO}
     * @param porcentaje vacío si la penalización es un monto fijo
     * @param montoFijo  vacío si la penalización es un porcentaje
     */
    public record PenalizacionResponse(String base, Integer porcentaje, BigDecimal montoFijo) {

        static PenalizacionResponse de(PoliticaResult.PenalizacionResult p) {
            return new PenalizacionResponse(p.base(), p.porcentaje(), p.montoFijo());
        }
    }

    static PoliticaPortalResponse de(PoliticaResult r) {
        return new PoliticaPortalResponse(
                r.tramos().stream()
                        .map(t -> new TramoResponse(t.antelacionMinHoras(), PenalizacionResponse.de(t.penalizacion())))
                        .toList(),
                PenalizacionResponse.de(r.penalizacionNoShow()));
    }
}
