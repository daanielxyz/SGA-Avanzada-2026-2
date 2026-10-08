package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import java.math.BigDecimal;

/**
 * Entrada de {@link DefinirTarifa} (CU-34): valor por ocupante facturable, por noche.
 */
public record DefinirTarifaCommand(String apartamento, String temporadaId, BigDecimal valorPorOcupante) {
}
