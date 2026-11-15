package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.util.List;

/**
 * VO: todo lo que el asistente necesita para responder una pregunta (DEC-127). Solo trae las secciones del manual del
 * rol de quien pregunta y los últimos mensajes; nada de huéspedes.
 *
 * @param solicitante quién pregunta, para el cupo de preguntas por hora; no se le envía al proveedor
 * @param pantalla    la ruta del PMS desde donde se pregunta, para entender «esto» o «aquí»; puede ir vacía
 * @param contexto    la configuración del alojamiento contada en texto ({@link ContextoAlojamientoDomainService})
 */
public record Consulta(UsuarioId solicitante, RolConsultante rol, Pregunta pregunta, String pantalla,
                       List<MensajeConversacion> historial, List<SeccionManual> secciones, String contexto) {

    /** Mensajes anteriores que se tienen en cuenta: los más recientes. */
    public static final int HISTORIAL_MAXIMO = 10;

    // DEC-127
    public Consulta {
        if (solicitante == null || rol == null || pregunta == null) {
            throw new ReglaDominioException("La consulta al asistente necesita quién pregunta, su rol y la pregunta");
        }
        pantalla = pantalla == null ? "" : pantalla.strip();
        List<MensajeConversacion> anteriores = historial == null ? List.of() : historial;
        historial = List.copyOf(anteriores.subList(Math.max(0, anteriores.size() - HISTORIAL_MAXIMO),
                anteriores.size()));
        secciones = secciones == null ? List.of() : secciones.stream().filter(s -> s.esPara(rol)).toList();
        contexto = contexto == null ? "" : contexto;
    }

    /** Si la sección con ese id es una de las que puede leer quien pregunta. */
    public boolean incluye(String seccionId) {
        return secciones.stream().anyMatch(seccion -> seccion.id().equals(seccionId));
    }
}
