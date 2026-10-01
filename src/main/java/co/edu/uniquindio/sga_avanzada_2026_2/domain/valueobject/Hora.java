package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Objeto de valor: Hora estimada de llegada u operación.
 */
public record Hora(LocalTime valor) {

    public Hora {
        Objects.requireNonNull(valor, "La hora no puede ser nula");
    }

    public static Hora de(int hora, int minuto) {
        return new Hora(LocalTime.of(hora, minuto));
    }

    public static Hora de(LocalTime localTime) {
        return new Hora(localTime);
    }
}
