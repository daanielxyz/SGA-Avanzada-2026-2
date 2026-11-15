package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.BusquedaTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: recepción encuentra a un titular por su nombre o su documento, o los ve a todos, de 10 en 10 y por
 * nombre (DEC-131 · DEC-18).
 */
@Service
public class BuscarTitularesUseCase {

    private final TitularRepository titulares;
    private final DirectorioPersonal personal;

    public BuscarTitularesUseCase(TitularRepository titulares, DirectorioPersonal personal) {
        this.titulares = titulares;
        this.personal = personal;
    }

    /**
     * @param texto        lo que se escribió; vacío trae a todos
     * @param numeroPagina empieza en 0
     */
    @Transactional(readOnly = true)
    public Pagina<TitularResult> ejecutar(String alojamientoId, String texto, int numeroPagina) {
        return titulares.buscar(new AlojamientoId(alojamientoId), BusquedaTitular.de(texto), numeroPagina)
                .map(titular -> TitularResult.de(titular, personal));
    }
}
