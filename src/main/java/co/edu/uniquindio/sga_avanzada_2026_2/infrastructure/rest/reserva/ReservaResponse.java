package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaConTitularResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * JSON de una reserva (DEC-31): estado, estancia, grupo, valor y política congelados (CU-49).
 *
 * @param salida     exclusiva: esa noche no se ocupa (EST-01)
 * @param valor      el desglose congelado al crear o modificar (RN-22)
 * @param politicaId la versión de la política de cancelación que aceptó el huésped
 * @param titularNombre solo en el listado de recepción y en el detalle; en las demás respuestas, nulo
 */
public record ReservaResponse(String codigo, String apartamento, String titularId, LocalDate entrada,
                              LocalDate salida, String estado, String canalOrigen,
                              List<OcupanteResponse> ocupantes, LocalTime horaEstimadaLlegada,
                              CotizacionResponse valor, String politicaId, LocalDateTime creadaEn,
                              String avisoCuenta, int vehiculos, int mascotas, String titularNombre) {

    public record OcupanteResponse(String id, String nombre, LocalDate fechaNacimiento, String tipoDocumento,
                                   String numeroDocumento, String nacionalidad) {
    }

    public static ReservaResponse de(ReservaResult r) {
        return de(r, null);
    }

    public static ReservaResponse de(ReservaConTitularResult r) {
        return de(r.reserva(), r.titularNombre());
    }

    private static ReservaResponse de(ReservaResult r, String titularNombre) {
        return new ReservaResponse(r.codigo(), r.apartamento(), r.titularId(), r.entrada(), r.salida(), r.estado(),
                r.canalOrigen(),
                r.ocupantes().stream()
                        .map(o -> new OcupanteResponse(o.id(), o.nombre(), o.fechaNacimiento(), o.tipoDocumento(),
                                o.numeroDocumento(), o.nacionalidad()))
                        .toList(),
                r.horaEstimadaLlegada(), CotizacionResponse.de(r.valor()), r.politicaId(), r.creadaEn(),
                r.avisoCuenta(), r.vehiculos(), r.mascotas(), titularNombre);
    }
}
