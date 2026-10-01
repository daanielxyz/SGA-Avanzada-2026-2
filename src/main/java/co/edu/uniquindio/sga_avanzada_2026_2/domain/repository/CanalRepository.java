package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CanalId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Canal.
 */
public interface CanalRepository {

    void guardar(Canal canal);

    Optional<Canal> buscarPorId(CanalId id);

    List<Canal> listarTodos();

    void eliminar(CanalId id);
}
