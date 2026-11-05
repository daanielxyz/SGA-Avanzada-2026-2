package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Salida de los casos de uso de Reserva (DEC-31): estado, estancia, grupo, valor y política congelados (CU-49).
 *
 * @param avisoCuenta solo al crear: avisa que el titular ya tiene una cuenta con otro correo (DEC-69); si no, nulo
 * @param vehiculos   carros y motos, que ocupan el mismo cupo (RP-02)
 * @param extras      lo que suman parqueadero y mascotas, aparte de {@code valor} (DEC-89)
 */
public record ReservaResult(String codigo, String apartamento, String titularId, LocalDate entrada,
                           LocalDate salida, String estado, String canalOrigen, List<OcupanteResult> ocupantes,
                           LocalTime horaEstimadaLlegada, CotizacionResult valor, String politicaId,
                           LocalDateTime creadaEn, String avisoCuenta, int vehiculos, int mascotas, int carros,
                           int motos, ExtrasResult extras) {

    public record ExtrasResult(BigDecimal parqueadero, BigDecimal mascotas, BigDecimal total) {
    }

    public record OcupanteResult(String id, String nombre, LocalDate fechaNacimiento, String tipoDocumento,
                                 String numeroDocumento, String nacionalidad) {
    }

    public static ReservaResult de(Reserva reserva) {
        return new ReservaResult(
                reserva.codigo().valor(),
                reserva.apartamentoId().valor(),
                reserva.titularId().valor(),
                reserva.estancia().entrada(),
                reserva.estancia().salida(),
                reserva.estado().name(),
                reserva.canalOrigen().name(),
                reserva.ocupantes().stream()
                        .map(o -> new OcupanteResult(o.id().valor(), o.nombre(), o.fechaNacimiento(),
                                Optional.ofNullable(o.documento()).map(d -> d.tipo().name()).orElse(null),
                                Optional.ofNullable(o.documento()).map(Documento::numero).orElse(null),
                                o.nacionalidad()))
                        .toList(),
                reserva.horaEstimadaLlegada(),
                CotizacionResult.de(reserva.desglose(), reserva.valorTotal().monto()),
                reserva.politicaVersionId().valor(),
                reserva.creadaEn(),
                null,
                reserva.vehiculos(),
                reserva.mascotas(),
                reserva.carros(),
                reserva.motos(),
                new ExtrasResult(reserva.extras().parqueadero().monto(), reserva.extras().mascotas().monto(),
                        reserva.extras().total().monto()));
    }

    public ReservaResult conAvisoCuenta(String aviso) {
        return new ReservaResult(codigo, apartamento, titularId, entrada, salida, estado, canalOrigen, ocupantes,
                horaEstimadaLlegada, valor, politicaId, creadaEn, aviso, vehiculos, mascotas, carros, motos, extras);
    }
}
