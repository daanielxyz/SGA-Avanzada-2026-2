package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: historial de novedades de un apartamento, de la más reciente a la más antigua y de 10 en 10 (CU-44 ·
 * NOV-06 · DEC-18). Incluye las cerradas.
 */
@Service
public class ConsultarNovedadesUseCase {

    private final NovedadRepository novedades;
    private final ApartamentoRepository apartamentos;

    public ConsultarNovedadesUseCase(NovedadRepository novedades, ApartamentoRepository apartamentos) {
        this.novedades = novedades;
        this.apartamentos = apartamentos;
    }

    /**
     * @param numeroPagina empieza en 0
     * @throws RecursoNoEncontradoException si no existe el apartamento
     */
    @Transactional(readOnly = true)
    public Pagina<NovedadResult> ejecutar(String apartamento, int numeroPagina) {
        ApartamentoId apartamentoId = new ApartamentoId(apartamento);
        apartamentos.buscarPorCodigo(apartamentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", apartamentoId.valor()));
        return novedades.listarPorApartamento(apartamentoId, numeroPagina).map(NovedadResult::de);
    }
}
