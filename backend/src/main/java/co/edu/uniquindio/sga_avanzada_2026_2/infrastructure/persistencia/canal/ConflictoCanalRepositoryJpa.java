package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EstadoConflicto;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link ConflictoCanalRepository} (DEC-13). No borra filas (CONF-05).
 */
@Repository
@Transactional
public class ConflictoCanalRepositoryJpa implements ConflictoCanalRepository {

    private final ConflictoCanalJpaRepository filas;

    public ConflictoCanalRepositoryJpa(ConflictoCanalJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(ConflictoCanal conflicto) {
        ConflictoCanalJpa fila = filas.findById(conflicto.id().valor()).orElseGet(ConflictoCanalJpa::new);
        CanalMapper.copiar(conflicto, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConflictoCanal> buscarPorId(ConflictoId id) {
        return filas.findById(id.valor()).map(CanalMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictoCanal> buscarPendientes() {
        return filas.findByEstadoOrderByDetectadoEnAsc(EstadoConflicto.PENDIENTE).stream()
                .map(CanalMapper::aDominio)
                .toList();
    }
}
