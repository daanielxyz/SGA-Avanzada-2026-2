package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Una fila del calendario de ocupación (CU-24): un apartamento, las reservas que dibuja en el rango y sus bloqueos.
 *
 * @param motivoFueraDeServicio solo mientras está FUERA_DE_SERVICIO, si se dio (DEC-113)
 */
public record OcupacionResult(String apartamento, String nombre, String estadoOperativo,
                              String motivoFueraDeServicio, List<ReservaOcupada> reservas,
                              List<BloqueoOcupado> bloqueos) {

    /**
     * @param saldo          siempre positivo; {@code situacionSaldo} dice en qué sentido (SLD-03)
     * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
     * @param totalPagado    pagos netos: con saldo PENDIENTE y esto en cero, el huésped debe todo (BL-09)
     */
    public record ReservaOcupada(String codigo, LocalDate entrada, LocalDate salida, String estado, String canalOrigen,
                                 String titularId, String titularNombre, BigDecimal saldo, String situacionSaldo,
                                 BigDecimal totalPagado) {
    }

    public record BloqueoOcupado(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo) {
    }
}
