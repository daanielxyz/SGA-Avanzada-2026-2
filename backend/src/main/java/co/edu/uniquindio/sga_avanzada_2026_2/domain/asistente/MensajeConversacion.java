package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: un mensaje anterior de la conversación, que el panel vuelve a enviar porque el modelo no recuerda nada entre
 * preguntas (DEC-127). También se enmascara: lo escribió una persona.
 */
public record MensajeConversacion(AutorMensaje autor, String texto) {

    /** Largo máximo de un mensaje del historial (una respuesta del asistente puede ser larga). */
    public static final int LARGO_MAXIMO = 4000;

    // DEC-127
    public MensajeConversacion {
        if (autor == null || texto == null || texto.isBlank()) {
            throw new ReglaDominioException("Cada mensaje del historial necesita su autor y su texto");
        }
        if (texto.length() > LARGO_MAXIMO) {
            throw new ReglaDominioException("Un mensaje del historial admite hasta " + LARGO_MAXIMO + " caracteres");
        }
        texto = DatosPersonales.enmascarar(texto.strip());
    }
}
