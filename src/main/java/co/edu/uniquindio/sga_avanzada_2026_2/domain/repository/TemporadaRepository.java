package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TemporadaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Temporada.
 */
public interface TemporadaRepository {

    void guardar(Temporada temporada);

    Optional<Temporada> buscarPorId(TemporadaId id);

    List<Temporada> listarTodos();

    void eliminar(TemporadaId id);
}
