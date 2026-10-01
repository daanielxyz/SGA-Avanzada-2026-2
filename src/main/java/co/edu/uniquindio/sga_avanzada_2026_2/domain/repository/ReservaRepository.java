package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TitularId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Reserva.
 */
public interface ReservaRepository {

    void guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(ReservaId id);

    List<Reserva> listarTodos();

    void eliminar(ReservaId id);

    List<Reserva> buscarPorApartamento(ApartamentoId apartamentoId);

    List<Reserva> buscarPorTitular(TitularId titularId);

    List<Reserva> buscarActivasByApartamento(ApartamentoId apartamentoId);
}
