package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: consultar los datos de un titular, por ejemplo el de una reserva.
 */
@Service
public class ConsultarTitular {

    private final TitularRepository titulares;

    public ConsultarTitular(TitularRepository titulares) {
        this.titulares = titulares;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     */
    @Transactional(readOnly = true)
    public TitularResult ejecutar(String id) {
        TitularId titularId = new TitularId(id);
        return titulares.buscarPorId(titularId)
                .map(TitularResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", titularId.valor()));
    }
}
