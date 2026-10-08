package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.time.LocalDateTime;

/**
 * Raíz del agregado EventoCanal: entrada de la bitácora de un canal, de solo escritura; no tiene métodos que la
 * cambien (BIT-02). Nunca guarda credenciales (BIT-04): la carga útil llega ya depurada desde el adaptador del canal.
 */
public class EventoCanal {

    private final EventoCanalId id;
    private final CanalId canalId;
    private final OperacionCanal operacion;
    private final SentidoEvento sentido;
    private final LocalDateTime fechaHora;
    private final String cargaUtil;
    private final ResultadoEvento resultado;
    private final String detalle; // opcional: motivo del rechazo o del error

    // BIT-01
    public EventoCanal(EventoCanalId id, CanalId canalId, OperacionCanal operacion, SentidoEvento sentido,
                       LocalDateTime fechaHora, String cargaUtil, ResultadoEvento resultado, String detalle) {
        if (id == null || canalId == null || operacion == null || sentido == null || fechaHora == null
                || cargaUtil == null || resultado == null) {
            throw new ReglaDominioException("El evento de canal requiere canal, operación, sentido, fecha y hora, "
                    + "carga útil y resultado");
        }
        this.id = id;
        this.canalId = canalId;
        this.operacion = operacion;
        this.sentido = sentido;
        this.fechaHora = fechaHora;
        this.cargaUtil = cargaUtil;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    public EventoCanalId id() {
        return id;
    }

    public CanalId canalId() {
        return canalId;
    }

    public OperacionCanal operacion() {
        return operacion;
    }

    public SentidoEvento sentido() {
        return sentido;
    }

    public LocalDateTime fechaHora() {
        return fechaHora;
    }

    public String cargaUtil() {
        return cargaUtil;
    }

    public ResultadoEvento resultado() {
        return resultado;
    }

    public String detalle() {
        return detalle;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EventoCanal otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
