package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ReservaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Folio.
 */
public interface FolioRepository {

    void guardar(Folio folio);

    Optional<Folio> buscarPorId(FolioId id);

    List<Folio> listarTodos();

    void eliminar(FolioId id);

    Optional<Folio> buscarPorReserva(ReservaId reservaId);
}
