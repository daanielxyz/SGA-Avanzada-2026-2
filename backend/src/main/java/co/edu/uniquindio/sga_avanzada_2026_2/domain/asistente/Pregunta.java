package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: lo que alguien del personal le pregunta al asistente, ya sin datos personales (DEC-127). El texto que guarda es
 * el enmascarado: el original no sale de aquí.
 */
public record Pregunta(String texto) {

    /** Largo máximo de una pregunta, en caracteres. */
    public static final int LARGO_MAXIMO = 1000;

    // DEC-127
    public Pregunta {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDominioException("Escribe la pregunta para el asistente");
        }
        if (texto.length() > LARGO_MAXIMO) {
            throw new ReglaDominioException("La pregunta admite hasta " + LARGO_MAXIMO + " caracteres");
        }
        texto = DatosPersonales.enmascarar(texto.strip());
    }
}
