package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TemporadaId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Temporada.
 */
public class TemporadaRepositoryEnMemoria implements TemporadaRepository {

    private final Map<TemporadaId, Temporada> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Temporada temporada) {
        Objects.requireNonNull(temporada, "La temporada no puede ser nula");
        almacenamiento.put(temporada.getId(), temporada);
    }

    @Override
    public Optional<Temporada> buscarPorId(TemporadaId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Temporada> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(TemporadaId id) {
        almacenamiento.remove(id);
    }
}
