package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NocheTest {

    @Test
    @Tag("NOC-01")
    void deberiaCrearNocheValidaAPartirDeFecha() {
        Noche noche = new Noche(LocalDate.of(2026, 10, 10));

        assertEquals(LocalDate.of(2026, 10, 10), noche.fecha());
        assertEquals(new Noche(LocalDate.of(2026, 10, 10)), noche);
    }

    @Test
    @Tag("NOC-01")
    void deberiaRechazarNocheSinFecha() {
        assertThrows(ReglaDominioException.class, () -> new Noche(null));
    }
}
