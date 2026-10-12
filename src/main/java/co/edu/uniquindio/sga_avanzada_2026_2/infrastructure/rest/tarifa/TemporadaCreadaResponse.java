package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.TemporadaCreadaResult;

import java.util.List;

/**
 * JSON de una temporada recién agregada con las tarifas que nacieron con ella (TAR-03).
 */
public record TemporadaCreadaResponse(CalendarioResponse.TemporadaResponse temporada, List<TarifaResponse> tarifas) {

    static TemporadaCreadaResponse de(TemporadaCreadaResult r) {
        return new TemporadaCreadaResponse(CalendarioResponse.TemporadaResponse.de(r.temporada()),
                r.tarifas().stream().map(TarifaResponse::de).toList());
    }
}
