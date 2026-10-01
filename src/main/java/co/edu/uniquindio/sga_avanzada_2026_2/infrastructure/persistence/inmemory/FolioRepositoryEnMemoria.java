package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ReservaId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Folio.
 */
public class FolioRepositoryEnMemoria implements FolioRepository {

    private final Map<FolioId, Folio> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Folio folio) {
        Objects.requireNonNull(folio, "El folio no puede ser nulo");
        almacenamiento.put(folio.getId(), folio);
    }

    @Override
    public Optional<Folio> buscarPorId(FolioId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Folio> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(FolioId id) {
        almacenamiento.remove(id);
    }

    @Override
    public Optional<Folio> buscarPorReserva(ReservaId reservaId) {
        Objects.requireNonNull(reservaId, "El reservaId no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(f -> f.getReservaId().equals(reservaId))
                .findFirst();
    }
}
