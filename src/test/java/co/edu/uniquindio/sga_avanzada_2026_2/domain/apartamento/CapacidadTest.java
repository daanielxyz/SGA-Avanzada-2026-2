package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacidadTest {

    @Test
    @Tag("CAP-01")
    void deberiaCrearCapacidadValida() {
        assertEquals(4, new Capacidad(4).valor());
    }

    @Test
    @Tag("CAP-01")
    void noPermitirCapacidadInvalida() {
        assertThrows(ReglaDominioException.class, () -> new Capacidad(0));
        assertThrows(ReglaDominioException.class, () -> new Capacidad(-2));
    }

    @Test
    @Tag("RN-02")
    @Tag("CAP-02")
    void deberiaAdmitirOcupantesHastaElTope() {
        assertTrue(new Capacidad(4).admite(4));
        assertTrue(new Capacidad(4).admite(1));
    }

    @Test
    @Tag("RN-02")
    @Tag("CAP-02")
    void noDeberiaAdmitirOcupantesPorEncimaDelTope() {
        assertFalse(new Capacidad(4).admite(5));
    }
}
