package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.PoliticaId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado PoliticaCancelacion.
 */
public interface PoliticaCancelacionRepository {

    void guardar(PoliticaCancelacion politica);

    Optional<PoliticaCancelacion> buscarPorId(PoliticaId id);

    List<PoliticaCancelacion> listarTodos();

    void eliminar(PoliticaId id);

    Optional<PoliticaCancelacion> buscarVigente();
}
