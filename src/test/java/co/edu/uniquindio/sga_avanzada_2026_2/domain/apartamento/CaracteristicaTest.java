package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaracteristicaTest {

    @Test
    @Tag("CARAC-01")
    void deberiaCrearCaracteristicaNormalizada() {
        assertEquals("Balcón", new Caracteristica("  Balcón ").nombre());
    }

    @Test
    @Tag("CARAC-01")
    void deberiaRechazarCaracteristicaVacia() {
        assertThrows(ReglaDominioException.class, () -> new Caracteristica(" "));
    }
}
