package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link AgregarTemporada} (CU-33): la temporada con la tarifa de cada apartamento activo (TAR-03).
 *
 * @param fechaFin             inclusiva (TEM-01)
 * @param estanciaMinimaNoches 0 si no exige mínimo (TEM-07)
 */
public record AgregarTemporadaCommand(String alojamientoId, String nombre, LocalDate fechaInicio,
                                      LocalDate fechaFin, int estanciaMinimaNoches, List<TarifaCommand> tarifas) {

    public record TarifaCommand(String apartamento, BigDecimal valorPorOcupante) {
    }
}
