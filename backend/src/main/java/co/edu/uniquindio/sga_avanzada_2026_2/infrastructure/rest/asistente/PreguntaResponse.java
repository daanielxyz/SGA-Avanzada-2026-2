package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.application.asistente.ResponderPreguntaResult;

import java.util.List;

/**
 * La respuesta del asistente del PMS (DEC-127).
 *
 * @param respuesta el texto, en Markdown sencillo (listas y negritas)
 * @param fuentes   las secciones del manual en que se basa, con su pantalla para llevar a ella
 * @param origen    {@code IA} o {@code LOCAL} (respondió el manual)
 * @param motivo    solo si es {@code LOCAL}: {@code IA_APAGADA}, {@code LIMITE_PERSONAL}, {@code CUOTA_AGOTADA} o
 *                  {@code NO_DISPONIBLE}
 * @param aviso     solo si es {@code LOCAL}: el texto para la persona que explica por qué respondió el manual
 */
public record PreguntaResponse(String respuesta, List<FuenteResponse> fuentes, String origen, String motivo,
                               String aviso) {

    /** @param pantalla la ruta del PMS, con {@code {codigo}} donde va el código de la reserva o del apartamento */
    public record FuenteResponse(String id, String titulo, String pantalla) {
    }

    static PreguntaResponse de(ResponderPreguntaResult result) {
        return new PreguntaResponse(result.respuesta(),
                result.fuentes().stream().map(f -> new FuenteResponse(f.id(), f.titulo(), f.pantalla())).toList(),
                result.origen(), result.motivo(), result.aviso());
    }
}
