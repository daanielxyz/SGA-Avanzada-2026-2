package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.TitularId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia para la raíz de agregado Titular.
 */
public interface TitularRepository {

    void guardar(Titular titular);

    Optional<Titular> buscarPorId(TitularId id);

    List<Titular> listarTodos();

    void eliminar(TitularId id);

    Optional<Titular> buscarPorDocumento(Documento documento);
}
