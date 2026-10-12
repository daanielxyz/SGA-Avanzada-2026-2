package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;

import java.time.LocalDateTime;

/**
 * Salida de la bitácora de un canal (DEC-31 · BIT-01).
 *
 * @param resultado EXITOSO, DUPLICADO, CONFLICTO, RECHAZADO o ERROR
 * @param detalle   el código de la reserva o el motivo del conflicto, del rechazo o del error
 */
public record EventoCanalResult(String id, String canalId, String operacion, String sentido, LocalDateTime fechaHora,
                                String cargaUtil, String resultado, String detalle) {

    public static EventoCanalResult de(EventoCanal evento) {
        return new EventoCanalResult(evento.id().valor(), evento.canalId().valor(), evento.operacion().name(),
                evento.sentido().name(), evento.fechaHora(), evento.cargaUtil(), evento.resultado().name(),
                evento.detalle());
    }
}
