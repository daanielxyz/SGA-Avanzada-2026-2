package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TarifaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Tarifa.
 */
public interface TarifaRepository {

    void guardar(Tarifa tarifa);

    Optional<Tarifa> buscarPorId(TarifaId id);

    List<Tarifa> listarTodos();

    void eliminar(TarifaId id);

    List<Tarifa> buscarPorApartamento(ApartamentoId apartamentoId);
}
