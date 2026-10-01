package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ConflictoId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad ConflictoCanal.
 */
public class ConflictoCanalRepositoryEnMemoria implements ConflictoCanalRepository {

    private final Map<ConflictoId, ConflictoCanal> almacenamiento = new HashMap<>();

    @Override
    public void guardar(ConflictoCanal conflicto) {
        Objects.requireNonNull(conflicto, "El conflicto no puede ser nulo");
        almacenamiento.put(conflicto.getId(), conflicto);
    }

    @Override
    public Optional<ConflictoCanal> buscarPorId(ConflictoId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<ConflictoCanal> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(ConflictoId id) {
        almacenamiento.remove(id);
    }
}
