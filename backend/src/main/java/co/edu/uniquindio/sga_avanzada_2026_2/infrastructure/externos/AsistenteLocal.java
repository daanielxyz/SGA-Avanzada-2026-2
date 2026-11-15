package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Asistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MotivoSinIa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RespuestaLocalDomainService;
import org.springframework.stereotype.Component;

/**
 * {@link Asistente} sin IA (DEC-127): responde con la sección del manual que más se parece a la pregunta. Es el de
 * siempre con {@code sga.asistente.modo=local} y en las pruebas; no sale a internet.
 */
@Component
public class AsistenteLocal implements Asistente {

    private final RespuestaLocalDomainService manual = new RespuestaLocalDomainService();

    @Override
    public Respuesta responder(Consulta consulta) {
        return manual.responder(consulta, MotivoSinIa.IA_APAGADA);
    }
}
