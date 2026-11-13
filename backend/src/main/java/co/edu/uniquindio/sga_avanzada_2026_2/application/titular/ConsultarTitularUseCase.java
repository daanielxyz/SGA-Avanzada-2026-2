package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: consultar los datos de un titular, por ejemplo el de una reserva, con el nombre de quien validó su
 * identidad (#15 · DEC-115).
 */
@Service
public class ConsultarTitularUseCase {

    private final TitularRepository titulares;
    private final DirectorioPersonal personal;

    public ConsultarTitularUseCase(TitularRepository titulares, DirectorioPersonal personal) {
        this.titulares = titulares;
        this.personal = personal;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el titular
     */
    @Transactional(readOnly = true)
    public TitularResult ejecutar(String id) {
        TitularId titularId = new TitularId(id);
        return titulares.buscarPorId(titularId)
                .map(titular -> TitularResult.de(titular, personal))
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", titularId.valor()));
    }
}
