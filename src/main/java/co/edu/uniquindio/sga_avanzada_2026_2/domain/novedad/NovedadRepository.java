package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado Novedad. No hay eliminación (NOV-06).
 */
public interface NovedadRepository {

    void guardar(Novedad novedad);

    Optional<Novedad> buscarPorId(NovedadId id);

    /**
     * Historial del apartamento, de la más reciente a la más antigua, de 10 en 10 (NOV-06 · CU-44 · DEC-18).
     *
     * @param numeroPagina empieza en 0
     */
    Pagina<Novedad> listarPorApartamento(ApartamentoId apartamentoId, int numeroPagina);
}
