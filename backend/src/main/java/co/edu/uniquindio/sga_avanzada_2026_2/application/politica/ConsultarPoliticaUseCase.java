package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver una versión de la política de cancelación, la que quedó congelada en una reserva (RN-13 · DEC-104).
 * Recepción la usa para explicarle al huésped cuánto se retiene si cancela o no llega.
 */
@Service
public class ConsultarPoliticaUseCase {

    private final PoliticaCancelacionRepository politicas;

    public ConsultarPoliticaUseCase(PoliticaCancelacionRepository politicas) {
        this.politicas = politicas;
    }

    /**
     * @throws RecursoNoEncontradoException si la versión no existe o es de otro alojamiento
     */
    @Transactional(readOnly = true)
    public PoliticaResult ejecutar(String alojamientoId, String politicaId) {
        AlojamientoId alojamiento = new AlojamientoId(alojamientoId);
        return politicas.buscarPorId(new PoliticaId(politicaId))
                .filter(politica -> politica.alojamientoId().equals(alojamiento))
                .map(PoliticaResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("la política", politicaId));
    }
}
