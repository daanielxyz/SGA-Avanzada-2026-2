package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupacionResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON de una fila del calendario de ocupación (CU-24).
 */
public record OcupacionResponse(String apartamento, String nombre, String estadoOperativo,
                                List<ReservaOcupadaResponse> reservas, List<BloqueoOcupadoResponse> bloqueos) {

    /**
     * @param estado         también FINALIZADA (ya salió) y NO_SHOW (no se presentó; sus noches están libres) (BL-09)
     * @param saldo          siempre positivo; {@code situacionSaldo} dice en qué sentido (SLD-03)
     * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
     * @param totalPagado    pagos netos: con saldo PENDIENTE y esto en cero, el huésped debe todo (BL-09)
     */
    public record ReservaOcupadaResponse(String codigo, LocalDate entrada, LocalDate salida, String estado,
                                         String canalOrigen, String titularId, String titularNombre, BigDecimal saldo,
                                         String situacionSaldo, BigDecimal totalPagado) {
    }

    public record BloqueoOcupadoResponse(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo) {
    }

    static OcupacionResponse de(OcupacionResult r) {
        return new OcupacionResponse(r.apartamento(), r.nombre(), r.estadoOperativo(),
                r.reservas().stream().map(x -> new ReservaOcupadaResponse(x.codigo(), x.entrada(), x.salida(),
                        x.estado(), x.canalOrigen(), x.titularId(), x.titularNombre(), x.saldo(), x.situacionSaldo(),
                        x.totalPagado())).toList(),
                r.bloqueos().stream().map(x -> new BloqueoOcupadoResponse(x.id(), x.fechaInicio(), x.fechaFin(),
                        x.motivo())).toList());
    }
}
