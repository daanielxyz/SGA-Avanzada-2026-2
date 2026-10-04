package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import java.time.LocalDateTime;

/**
 * Raíz del agregado EventoCanal (bitácora): solo se crea y nunca guarda credenciales.
 */
public class EventoCanal {

    private final EventoCanalId id;
    private final CanalId canalId;
    private final String operacion;
    private final SentidoEvento sentido;
    private final LocalDateTime fechaHora;
    private final String cargaUtil;
    private final String resultado;

    public EventoCanal(EventoCanalId id, CanalId canalId, String operacion, SentidoEvento sentido,
                       LocalDateTime fechaHora, String cargaUtil, String resultado) {
        this.id = id;
        this.canalId = canalId;
        this.operacion = operacion;
        this.sentido = sentido;
        this.fechaHora = fechaHora;
        this.cargaUtil = cargaUtil;
        this.resultado = resultado;
    }
}
