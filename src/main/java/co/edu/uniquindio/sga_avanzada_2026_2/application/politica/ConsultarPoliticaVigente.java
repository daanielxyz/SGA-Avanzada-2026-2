package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver la política de cancelación vigente, la que acepta el huésped al reservar (POL-01 · DEC-42).
 */
@Service
public class ConsultarPoliticaVigente {

    private final PoliticaCancelacionRepository politicas;

    public ConsultarPoliticaVigente(PoliticaCancelacionRepository politicas) {
        this.politicas = politicas;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento todavía no tiene política
     */
    @Transactional(readOnly = true)
    public PoliticaResult ejecutar(String alojamientoId) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        return politicas.buscarVigente(id)
                .map(PoliticaResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("la política vigente del alojamiento",
                        id.valor()));
    }
}
