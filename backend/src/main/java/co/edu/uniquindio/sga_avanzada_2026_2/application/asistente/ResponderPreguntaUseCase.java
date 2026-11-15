package co.edu.uniquindio.sga_avanzada_2026_2.application.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Asistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AsistenteNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AutorMensaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.ContextoAlojamientoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.ManualAsistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MensajeConversacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Pregunta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RespuestaLocalDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RolConsultante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: responder una duda de uso del PMS (DEC-127). Carga la configuración del alojamiento y el manual, arma
 * la consulta con lo que puede leer el rol de quien pregunta y se la pasa al asistente. Si la IA no puede responder,
 * responde el manual: el PMS nunca depende de la IA. No guarda nada.
 */
@Service
public class ResponderPreguntaUseCase {

    private final AlojamientoRepository alojamientos;
    private final PoliticaCancelacionRepository politicas;
    private final ManualAsistente manual;
    private final Asistente asistente;
    private final ContextoAlojamientoDomainService contexto = new ContextoAlojamientoDomainService();
    private final RespuestaLocalDomainService local = new RespuestaLocalDomainService();

    public ResponderPreguntaUseCase(AlojamientoRepository alojamientos, PoliticaCancelacionRepository politicas,
                                    ManualAsistente manual, Asistente asistente) {
        this.alojamientos = alojamientos;
        this.politicas = politicas;
        this.manual = manual;
        this.asistente = asistente;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     */
    // DEC-127
    @Transactional(readOnly = true)
    public ResponderPreguntaResult ejecutar(ResponderPreguntaCommand command) {
        AlojamientoId alojamientoId = new AlojamientoId(command.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId.valor()));
        Consulta consulta = new Consulta(new UsuarioId(command.solicitante()),
                RolConsultante.valueOf(command.rol()), new Pregunta(command.pregunta()), command.pantalla(),
                historial(command.historial()), manual.secciones(),
                contexto.describir(alojamiento, politicas.buscarVigente(alojamientoId)));
        return ResponderPreguntaResult.de(responder(consulta), consulta);
    }

    private Respuesta responder(Consulta consulta) {
        try {
            return asistente.responder(consulta);
        } catch (AsistenteNoDisponibleException sinIa) {
            return local.responder(consulta, sinIa.motivo());
        }
    }

    private static List<MensajeConversacion> historial(List<ResponderPreguntaCommand.Mensaje> mensajes) {
        return mensajes == null ? List.of() : mensajes.stream()
                .map(m -> new MensajeConversacion(AutorMensaje.valueOf(m.autor()), m.texto()))
                .toList();
    }
}
