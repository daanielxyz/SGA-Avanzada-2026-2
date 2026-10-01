package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CanalId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Canal.
 */
public class CanalRepositoryEnMemoria implements CanalRepository {

    private final Map<CanalId, Canal> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Canal canal) {
        Objects.requireNonNull(canal, "El canal no puede ser nulo");
        almacenamiento.put(canal.getId(), canal);
    }

    @Override
    public Optional<Canal> buscarPorId(CanalId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Canal> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(CanalId id) {
        almacenamiento.remove(id);
    }
}
