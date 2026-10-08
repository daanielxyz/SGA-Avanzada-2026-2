package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult.TemporadaResult;

import java.util.List;

/**
 * Salida de {@link AgregarTemporada}: la temporada y las tarifas que nacieron con ella (TAR-03).
 */
public record TemporadaCreadaResult(TemporadaResult temporada, List<TarifaResult> tarifas) {
}
