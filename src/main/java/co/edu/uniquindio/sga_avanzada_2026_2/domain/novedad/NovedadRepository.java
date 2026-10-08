package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Novedad. No hay eliminación (NOV-06).
 */
public interface NovedadRepository {

    void guardar(Novedad novedad);

    Optional<Novedad> buscarPorId(NovedadId id);

    /** Historial completo del apartamento, de la más reciente a la más antigua (NOV-06 · CU-44). */
    List<Novedad> buscarPorApartamento(ApartamentoId apartamentoId);
}
