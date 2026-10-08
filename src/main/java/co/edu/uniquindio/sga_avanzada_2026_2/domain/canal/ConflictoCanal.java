package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Raíz del agregado ConflictoCanal: una reserva externa rechazada por falta de disponibilidad (RN-18 · CONF-01 ·
 * DEC-48). Nunca toca la reserva vigente (CONF-02) ni se elimina: es evidencia de auditoría (CONF-05).
 */
public class ConflictoCanal {

    private final ConflictoId id;
    private final CanalId canalId;
    private final String idExterno;
    private final ApartamentoId apartamentoId;
    private final Estancia estanciaSolicitada;
    private final LocalDateTime detectadoEn;
    private final String motivo;
    private EstadoConflicto estado;
    private UsuarioId resueltoPor;     // solo RESUELTO
    private LocalDateTime resueltoEn;  // solo RESUELTO
    private String decision;           // solo RESUELTO

    // CONF-03 · CONF-04
    public ConflictoCanal(ConflictoId id, CanalId canalId, String idExterno, ApartamentoId apartamentoId,
                          Estancia estanciaSolicitada, LocalDateTime detectadoEn, String motivo,
                          EstadoConflicto estado, UsuarioId resueltoPor, LocalDateTime resueltoEn, String decision) {
        if (id == null || canalId == null || apartamentoId == null || estanciaSolicitada == null
                || detectadoEn == null || estado == null) {
            throw new ReglaDominioException("Faltan datos obligatorios del conflicto de canal");
        }
        if (idExterno == null || idExterno.isBlank() || motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("El conflicto requiere el identificador externo y el motivo del rechazo");
        }
        boolean resuelto = estado == EstadoConflicto.RESUELTO;
        if (resuelto != (resueltoPor != null && resueltoEn != null && decision != null)) {
            throw new ReglaDominioException("Solo un conflicto RESUELTO tiene autor, fecha y decisión");
        }
        this.id = id;
        this.canalId = canalId;
        this.idExterno = idExterno.trim();
        this.apartamentoId = apartamentoId;
        this.estanciaSolicitada = estanciaSolicitada;
        this.detectadoEn = detectadoEn;
        this.motivo = motivo.trim();
        this.estado = estado;
        this.resueltoPor = resueltoPor;
        this.resueltoEn = resueltoEn;
        this.decision = decision;
    }

    /**
     * Registra un conflicto PENDIENTE con lo que pidió el canal y por qué se rechazó (CONF-01 · CONF-03).
     *
     * @throws ReglaDominioException si falta algún dato (CONF-03)
     */
    public static ConflictoCanal registrar(ConflictoId id, CanalId canalId, String idExterno,
                                           ApartamentoId apartamentoId, Estancia estanciaSolicitada,
                                           LocalDateTime detectadoEn, String motivo) {
        return new ConflictoCanal(id, canalId, idExterno, apartamentoId, estanciaSolicitada, detectadoEn, motivo,
                EstadoConflicto.PENDIENTE, null, null, null);
    }

    /**
     * Marca el conflicto como RESUELTO con autor, fecha y la decisión tomada (CONF-04). No modifica ninguna reserva
     * (CONF-02); que solo lo haga el administrador (CAN-06) se controla en la aplicación (DEC-22).
     *
     * @throws ReglaDominioException si ya estaba resuelto o falta la decisión
     */
    public void resolver(UsuarioId autor, String decisionTomada, LocalDateTime ahora) {
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(ahora, "ahora");
        if (estado == EstadoConflicto.RESUELTO) {
            throw new ReglaDominioException("El conflicto " + id.valor() + " ya está resuelto");
        }
        if (decisionTomada == null || decisionTomada.isBlank()) {
            throw new ReglaDominioException("Resolver un conflicto requiere registrar la decisión tomada");
        }
        resueltoPor = autor;
        resueltoEn = ahora;
        decision = decisionTomada.trim();
        estado = EstadoConflicto.RESUELTO;
    }

    public ConflictoId id() {
        return id;
    }

    public CanalId canalId() {
        return canalId;
    }

    public String idExterno() {
        return idExterno;
    }

    public ApartamentoId apartamentoId() {
        return apartamentoId;
    }

    public Estancia estanciaSolicitada() {
        return estanciaSolicitada;
    }

    public LocalDateTime detectadoEn() {
        return detectadoEn;
    }

    public String motivo() {
        return motivo;
    }

    public EstadoConflicto estado() {
        return estado;
    }

    public UsuarioId resueltoPor() {
        return resueltoPor;
    }

    public LocalDateTime resueltoEn() {
        return resueltoEn;
    }

    public String decision() {
        return decision;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConflictoCanal otro && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
