package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

/**
 * Convierte Canal, ConflictoCanal y EventoCanal ⇄ sus filas JPA. Al cargar usa los constructores de reconstrucción
 * del dominio (DEC-06).
 */
final class CanalMapper {

    private CanalMapper() {
    }

    static Canal aDominio(CanalJpa jpa) {
        return new Canal(new CanalId(jpa.getId()), new AlojamientoId(jpa.getAlojamientoId()), jpa.getNombre(),
                jpa.getTipo(), jpa.getReferenciaCredencial(), jpa.isActivo());
    }

    /** Copia sobre una fila nueva o ya cargada; no toca {@code version}. */
    static void copiar(Canal canal, CanalJpa jpa) {
        jpa.setId(canal.id().valor());
        jpa.setAlojamientoId(canal.alojamientoId().valor());
        jpa.setNombre(canal.nombre());
        jpa.setTipo(canal.tipo());
        jpa.setReferenciaCredencial(canal.referenciaCredencial());
        jpa.setActivo(canal.activo());
    }

    static ConflictoCanal aDominio(ConflictoCanalJpa jpa) {
        return new ConflictoCanal(new ConflictoId(jpa.getId()), new CanalId(jpa.getCanalId()), jpa.getIdExterno(),
                new ApartamentoId(jpa.getApartamentoCodigo()), new Estancia(jpa.getEntrada(), jpa.getSalida()),
                jpa.getDetectadoEn(), jpa.getMotivo(), jpa.getEstado(),
                jpa.getResueltoPor() == null ? null : new UsuarioId(jpa.getResueltoPor()), jpa.getResueltoEn(),
                jpa.getDecision());
    }

    /** Copia sobre una fila nueva o ya cargada; no toca {@code version}. */
    static void copiar(ConflictoCanal conflicto, ConflictoCanalJpa jpa) {
        jpa.setId(conflicto.id().valor());
        jpa.setCanalId(conflicto.canalId().valor());
        jpa.setIdExterno(conflicto.idExterno());
        jpa.setApartamentoCodigo(conflicto.apartamentoId().valor());
        jpa.setEntrada(conflicto.estanciaSolicitada().entrada());
        jpa.setSalida(conflicto.estanciaSolicitada().salida());
        jpa.setDetectadoEn(conflicto.detectadoEn());
        jpa.setMotivo(conflicto.motivo());
        jpa.setEstado(conflicto.estado());
        jpa.setResueltoPor(conflicto.resueltoPor() == null ? null : conflicto.resueltoPor().valor());
        jpa.setResueltoEn(conflicto.resueltoEn());
        jpa.setDecision(conflicto.decision());
    }

    static EventoCanal aDominio(EventoCanalJpa jpa) {
        return new EventoCanal(new EventoCanalId(jpa.getId()), new CanalId(jpa.getCanalId()), jpa.getOperacion(),
                jpa.getSentido(), jpa.getFechaHora(), jpa.getCargaUtil(), jpa.getResultado(), jpa.getDetalle());
    }

    static EventoCanalJpa aJpa(EventoCanal evento) {
        return new EventoCanalJpa(evento.id().valor(), evento.canalId().valor(), evento.operacion(), evento.sentido(),
                evento.fechaHora(), evento.cargaUtil(), evento.resultado(), evento.detalle());
    }
}
