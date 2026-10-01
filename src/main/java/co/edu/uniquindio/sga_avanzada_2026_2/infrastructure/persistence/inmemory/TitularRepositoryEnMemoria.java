package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.TitularRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TitularId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Titular.
 */
public class TitularRepositoryEnMemoria implements TitularRepository {

    private final Map<TitularId, Titular> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Titular titular) {
        Objects.requireNonNull(titular, "El titular no puede ser nulo");
        almacenamiento.put(titular.getId(), titular);
    }

    @Override
    public Optional<Titular> buscarPorId(TitularId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Titular> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(TitularId id) {
        almacenamiento.remove(id);
    }

    @Override
    public Optional<Titular> buscarPorDocumento(Documento documento) {
        Objects.requireNonNull(documento, "El documento no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(t -> t.getDocumento().equals(documento))
                .findFirst();
    }
}
