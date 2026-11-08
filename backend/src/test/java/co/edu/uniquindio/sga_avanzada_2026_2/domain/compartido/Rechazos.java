package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Comprueba que una regla se viole con el motivo que las pantallas esperan. */
public final class Rechazos {

    private Rechazos() {
    }

    public static void exigeMotivo(MotivoRechazo motivo, Executable accion) {
        assertEquals(Optional.of(motivo), assertThrows(ReglaDominioException.class, accion).motivo());
    }
}
