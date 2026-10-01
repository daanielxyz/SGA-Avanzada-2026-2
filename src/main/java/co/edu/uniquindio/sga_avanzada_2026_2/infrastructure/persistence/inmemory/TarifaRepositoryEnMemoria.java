package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TarifaId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Tarifa.
 */
public class TarifaRepositoryEnMemoria implements TarifaRepository {

    private final Map<TarifaId, Tarifa> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Tarifa tarifa) {
        Objects.requireNonNull(tarifa, "La tarifa no puede ser nula");
        almacenamiento.put(tarifa.getId(), tarifa);
    }

    @Override
    public Optional<Tarifa> buscarPorId(TarifaId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Tarifa> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(TarifaId id) {
        almacenamiento.remove(id);
    }

    @Override
    public List<Tarifa> buscarPorApartamento(ApartamentoId apartamentoId) {
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(t -> t.getApartamentoId().equals(apartamentoId))
                .toList();
    }
}
