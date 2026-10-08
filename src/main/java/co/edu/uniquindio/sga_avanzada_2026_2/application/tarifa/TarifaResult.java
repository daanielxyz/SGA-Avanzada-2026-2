package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salida de los casos de uso de Tarifa (DEC-31): una versión de la tarifa de un apartamento en una temporada.
 */
public record TarifaResult(String id, String apartamento, String temporada, BigDecimal valorPorOcupante,
                           int version, LocalDate vigenteDesde) {

    static TarifaResult de(Tarifa tarifa) {
        return new TarifaResult(tarifa.id().valor(), tarifa.apartamentoId().valor(), tarifa.temporadaId().valor(),
                tarifa.valorPorOcupante().monto(), tarifa.version(), tarifa.vigenteDesde());
    }
}
