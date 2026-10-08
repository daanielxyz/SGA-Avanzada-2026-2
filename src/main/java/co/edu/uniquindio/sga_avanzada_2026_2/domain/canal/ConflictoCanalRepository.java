package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado ConflictoCanal. No hay eliminación: es evidencia de auditoría (CONF-05).
 */
public interface ConflictoCanalRepository {

    void guardar(ConflictoCanal conflicto);

    Optional<ConflictoCanal> buscarPorId(ConflictoId id);

    /** Los que esperan revisión del administrador (CONF-04). */
    List<ConflictoCanal> buscarPendientes();
}
