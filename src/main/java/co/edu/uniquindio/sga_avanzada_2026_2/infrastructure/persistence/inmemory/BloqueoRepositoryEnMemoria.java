package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.BloqueoId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para Bloqueos.
 */
public class BloqueoRepositoryEnMemoria implements BloqueoRepository {

    private final Map<ApartamentoId, List<Bloqueo>> almacenamiento = new HashMap<>();

    @Override
    public void guardar(ApartamentoId apartamentoId, Bloqueo bloqueo) {
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        Objects.requireNonNull(bloqueo, "El bloqueo no puede ser nulo");
        almacenamiento.computeIfAbsent(apartamentoId, k -> new ArrayList<>()).add(bloqueo);
    }

    @Override
    public Optional<Bloqueo> buscarPorId(BloqueoId id) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        return almacenamiento.values().stream()
                .flatMap(List::stream)
                .filter(b -> b.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Bloqueo> buscarVigentesByApartamento(ApartamentoId apartamentoId) {
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        List<Bloqueo> bloqueos = almacenamiento.getOrDefault(apartamentoId, Collections.emptyList());
        return bloqueos.stream().filter(Bloqueo::isVigente).toList();
    }

    @Override
    public void eliminar(BloqueoId id) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        almacenamiento.values().forEach(lista -> lista.removeIf(b -> b.getId().equals(id)));
    }
}
