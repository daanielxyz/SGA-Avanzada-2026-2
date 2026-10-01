package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ConflictoId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado ConflictoCanal.
 */
public interface ConflictoCanalRepository {

    void guardar(ConflictoCanal conflicto);

    Optional<ConflictoCanal> buscarPorId(ConflictoId id);

    List<ConflictoCanal> listarTodos();

    void eliminar(ConflictoId id);
}
