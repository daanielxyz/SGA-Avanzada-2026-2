package co.edu.uniquindio.sga_avanzada_2026_2.application.asistente;

import java.util.List;

/**
 * Entrada de {@link ResponderPreguntaUseCase} (DEC-127).
 *
 * @param solicitante el id del usuario de la sesión, nunca del cuerpo de la petición
 * @param rol         el rol de la sesión: RECEPCIONISTA, PERSONAL_SERVICIO o ADMINISTRADOR
 * @param historial   los mensajes anteriores de la conversación, del más antiguo al más reciente
 */
public record ResponderPreguntaCommand(String alojamientoId, String solicitante, String rol, String pregunta,
                                       String pantalla, List<Mensaje> historial) {

    /** @param autor PERSONA o ASISTENTE */
    public record Mensaje(String autor, String texto) {
    }
}
