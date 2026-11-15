package co.edu.uniquindio.sga_avanzada_2026_2.application.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MotivoSinIa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.SeccionManual;

import java.util.List;

/**
 * Salida de {@link ResponderPreguntaUseCase} (DEC-127).
 *
 * @param origen IA o LOCAL
 * @param motivo solo si es LOCAL: IA_APAGADA, LIMITE_PERSONAL, CUOTA_AGOTADA o NO_DISPONIBLE
 * @param aviso  solo si es LOCAL: el texto que explica a la persona por qué responde el manual
 */
public record ResponderPreguntaResult(String respuesta, List<Fuente> fuentes, String origen, String motivo,
                                      String aviso) {

    /** Una sección del manual citada, con su pantalla para llevar a ella. */
    public record Fuente(String id, String titulo, String pantalla) {
    }

    static ResponderPreguntaResult de(Respuesta respuesta, Consulta consulta) {
        List<Fuente> fuentes = respuesta.fuentes().stream()
                .flatMap(id -> consulta.secciones().stream().filter(seccion -> seccion.id().equals(id)))
                .map(ResponderPreguntaResult::fuente)
                .toList();
        return new ResponderPreguntaResult(respuesta.texto(), fuentes, respuesta.origen().name(),
                respuesta.motivoSinIa().map(MotivoSinIa::name).orElse(null),
                respuesta.motivoSinIa().map(MotivoSinIa::aviso).orElse(null));
    }

    private static Fuente fuente(SeccionManual seccion) {
        return new Fuente(seccion.id(), seccion.titulo(), seccion.pantalla());
    }
}
