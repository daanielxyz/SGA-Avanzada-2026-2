package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador JPA del puerto {@link CanalRepository} (DEC-13).
 */
@Repository
@Transactional
public class CanalRepositoryJpa implements CanalRepository {

    private final CanalJpaRepository filas;

    public CanalRepositoryJpa(CanalJpaRepository filas) {
        this.filas = filas;
    }

    /** Inserta o actualiza reutilizando la fila cargada, para conservar su {@code version} (DEC-17). */
    @Override
    public void guardar(Canal canal) {
        CanalJpa fila = filas.findById(canal.id().valor()).orElseGet(CanalJpa::new);
        CanalMapper.copiar(canal, fila);
        filas.save(fila);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Canal> buscarPorId(CanalId id) {
        return filas.findById(id.valor()).map(CanalMapper::aDominio);
    }
}
