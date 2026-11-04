package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.EstanciaResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JSON de la llegada, su corrección y la salida (DEC-31): cómo quedan juntos la reserva, el apartamento y el folio
 * (D-02).
 *
 * @param registros      historial de registros de llegada; el vigente es el único no anulado (REG-06)
 * @param salida         solo cuando la reserva está FINALIZADA
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 */
public record EstanciaResponse(String codigo, String estado, String apartamento, String estadoOperativo,
                               List<RegistroResponse> registros, SalidaResponse salida, boolean folioCerrado,
                               BigDecimal saldo, String situacionSaldo) {

    public record RegistroResponse(String id, LocalDateTime fechaHora, String autor, boolean anulado,
                                   String motivoCorreccion) {
    }

    public record SalidaResponse(String id, LocalDateTime fechaHora, String autor) {
    }

    static EstanciaResponse de(EstanciaResult r) {
        return new EstanciaResponse(r.codigo(), r.estado(), r.apartamento(), r.estadoOperativo(),
                r.registros().stream()
                        .map(g -> new RegistroResponse(g.id(), g.fechaHora(), g.autor(), g.anulado(),
                                g.motivoCorreccion()))
                        .toList(),
                Optional.ofNullable(r.salida())
                        .map(s -> new SalidaResponse(s.id(), s.fechaHora(), s.autor()))
                        .orElse(null),
                r.folioCerrado(), r.saldo(), r.situacionSaldo());
    }
}
