package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.EventoCanalResult;

import java.time.LocalDateTime;

/**
 * JSON de un evento de la bitácora de un canal (DEC-31 · BIT-01).
 *
 * @param resultado EXITOSO, DUPLICADO, CONFLICTO, RECHAZADO o ERROR
 * @param detalle   el código de la reserva o el motivo del conflicto, del rechazo o del error
 */
public record EventoCanalResponse(String id, String operacion, String sentido, LocalDateTime fechaHora,
                                  String cargaUtil, String resultado, String detalle) {

    static EventoCanalResponse de(EventoCanalResult r) {
        return new EventoCanalResponse(r.id(), r.operacion(), r.sentido(), r.fechaHora(), r.cargaUtil(),
                r.resultado(), r.detalle());
    }
}
