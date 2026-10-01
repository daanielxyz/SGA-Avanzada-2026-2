package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.PoliticaId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad PoliticaCancelacion.
 */
public class PoliticaCancelacionRepositoryEnMemoria implements PoliticaCancelacionRepository {

    private final Map<PoliticaId, PoliticaCancelacion> almacenamiento = new HashMap<>();

    @Override
    public void guardar(PoliticaCancelacion politica) {
        Objects.requireNonNull(politica, "La política no puede ser nula");
        almacenamiento.put(politica.getId(), politica);
    }

    @Override
    public Optional<PoliticaCancelacion> buscarPorId(PoliticaId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<PoliticaCancelacion> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(PoliticaId id) {
        almacenamiento.remove(id);
    }

    @Override
    public Optional<PoliticaCancelacion> buscarVigente() {
        return almacenamiento.values().stream()
                .filter(PoliticaCancelacion::isVigente)
                .findFirst();
    }
}
