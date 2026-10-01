package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Apartamento.
 */
public class ApartamentoRepositoryEnMemoria implements ApartamentoRepository {

    private final Map<ApartamentoId, Apartamento> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Apartamento apartamento) {
        Objects.requireNonNull(apartamento, "El apartamento no puede ser nulo");
        almacenamiento.put(apartamento.getCodigo(), apartamento);
    }

    @Override
    public Optional<Apartamento> buscarPorId(ApartamentoId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Apartamento> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(ApartamentoId id) {
        almacenamiento.remove(id);
    }
}
