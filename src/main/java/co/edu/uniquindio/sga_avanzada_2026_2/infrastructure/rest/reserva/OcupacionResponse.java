package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupacionResult;

import java.time.LocalDate;
import java.util.List;

/**
 * JSON de una fila del calendario de ocupación (CU-24).
 */
public record OcupacionResponse(String apartamento, String nombre, String estadoOperativo,
                                List<ReservaOcupadaResponse> reservas, List<BloqueoOcupadoResponse> bloqueos) {

    public record ReservaOcupadaResponse(String codigo, LocalDate entrada, LocalDate salida, String estado,
                                         String canalOrigen, String titularId) {
    }

    public record BloqueoOcupadoResponse(String id, LocalDate fechaInicio, LocalDate fechaFin, String motivo) {
    }

    static OcupacionResponse de(OcupacionResult r) {
        return new OcupacionResponse(r.apartamento(), r.nombre(), r.estadoOperativo(),
                r.reservas().stream().map(x -> new ReservaOcupadaResponse(x.codigo(), x.entrada(), x.salida(),
                        x.estado(), x.canalOrigen(), x.titularId())).toList(),
                r.bloqueos().stream().map(x -> new BloqueoOcupadoResponse(x.id(), x.fechaInicio(), x.fechaFin(),
                        x.motivo())).toList());
    }
}
