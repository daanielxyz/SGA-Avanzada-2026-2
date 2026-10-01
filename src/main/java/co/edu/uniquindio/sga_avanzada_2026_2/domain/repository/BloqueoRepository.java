package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.BloqueoId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de Dominio (Repository): Gestión de persistencia de bloqueos por apartamento.
 */
public interface BloqueoRepository {

    void guardar(ApartamentoId apartamentoId, Bloqueo bloqueo);

    Optional<Bloqueo> buscarPorId(BloqueoId id);

    List<Bloqueo> buscarVigentesByApartamento(ApartamentoId apartamentoId);

    void eliminar(BloqueoId id);
}
