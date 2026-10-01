package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Apartamento.
 */
public interface ApartamentoRepository {

    void guardar(Apartamento apartamento);

    Optional<Apartamento> buscarPorId(ApartamentoId id);

    List<Apartamento> listarTodos();

    void eliminar(ApartamentoId id);
}
