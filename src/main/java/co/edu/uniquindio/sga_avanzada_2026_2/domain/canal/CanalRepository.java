package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Canal.
 */
public interface CanalRepository {

    void guardar(Canal canal);

    Optional<Canal> buscarPorId(CanalId id);

    /** Los canales del alojamiento, activos o no, ordenados por id (CU-54). */
    List<Canal> buscarPorAlojamiento(AlojamientoId alojamientoId);
}
