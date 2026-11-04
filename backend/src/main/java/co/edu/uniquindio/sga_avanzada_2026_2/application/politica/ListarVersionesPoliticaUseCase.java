package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: las versiones de la política de cancelación, de la más alta a la más baja y de 10 en 10 (CU-35 · POL-04
 * · DEC-18). Al publicar una nueva, las anteriores se conservan.
 */
@Service
public class ListarVersionesPoliticaUseCase {

    private final PoliticaCancelacionRepository politicas;

    public ListarVersionesPoliticaUseCase(PoliticaCancelacionRepository politicas) {
        this.politicas = politicas;
    }

    @Transactional(readOnly = true)
    public Pagina<PoliticaResult> ejecutar(String alojamientoId, int numeroPagina) {
        return politicas.listarVersiones(new AlojamientoId(alojamientoId), numeroPagina).map(PoliticaResult::de);
    }
}
