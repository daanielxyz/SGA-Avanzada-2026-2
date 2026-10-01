package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TitularId;

import java.util.*;

/**
 * Adaptador de Infraestructura: Repositorio en memoria con HashMap para la entidad Reserva.
 */
public class ReservaRepositoryEnMemoria implements ReservaRepository {

    private final Map<ReservaId, Reserva> almacenamiento = new HashMap<>();

    @Override
    public void guardar(Reserva reserva) {
        Objects.requireNonNull(reserva, "La reserva no puede ser nula");
        almacenamiento.put(reserva.getCodigo(), reserva);
    }

    @Override
    public Optional<Reserva> buscarPorId(ReservaId id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public List<Reserva> listarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public void eliminar(ReservaId id) {
        almacenamiento.remove(id);
    }

    @Override
    public List<Reserva> buscarPorApartamento(ApartamentoId apartamentoId) {
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(r -> r.getApartamentoId().equals(apartamentoId))
                .toList();
    }

    @Override
    public List<Reserva> buscarPorTitular(TitularId titularId) {
        Objects.requireNonNull(titularId, "El titularId no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(r -> r.getTitularId().equals(titularId))
                .toList();
    }

    @Override
    public List<Reserva> buscarActivasByApartamento(ApartamentoId apartamentoId) {
        Objects.requireNonNull(apartamentoId, "El apartamentoId no puede ser nulo");
        return almacenamiento.values().stream()
                .filter(r -> r.getApartamentoId().equals(apartamentoId))
                .filter(Reserva::estaActiva)
                .toList();
    }
}
