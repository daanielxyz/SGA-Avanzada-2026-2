package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuántas preguntas a la IA le quedan a cada persona en la última hora (DEC-127), para que una sola sesión no gaste la
 * cuota gratis de todo el proyecto. Vive en la memoria del backend: con una sola instancia (Render) basta, y al
 * reiniciar vuelve a empezar, lo que no hace daño.
 */
class CupoPreguntas {

    private static final Duration VENTANA = Duration.ofHours(1);

    private final int maximo;
    private final Map<String, Deque<Instant>> preguntas = new ConcurrentHashMap<>();

    CupoPreguntas(int maximo) {
        this.maximo = maximo;
    }

    /**
     * Cuenta una pregunta de esa persona si todavía tiene cupo.
     *
     * @return falso si ya hizo {@code maximo} preguntas en la última hora: esta no se cuenta
     */
    boolean tomar(String persona, Instant ahora) {
        Deque<Instant> suyas = preguntas.computeIfAbsent(persona, clave -> new ArrayDeque<>());
        synchronized (suyas) {
            while (!suyas.isEmpty() && !suyas.peekFirst().isAfter(ahora.minus(VENTANA))) {
                suyas.pollFirst();
            }
            if (suyas.size() >= maximo) {
                return false;
            }
            suyas.addLast(ahora);
            return true;
        }
    }
}
