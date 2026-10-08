package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado Canal.
 */
public interface CanalRepository {

    void guardar(Canal canal);

    Optional<Canal> buscarPorId(CanalId id);
}
