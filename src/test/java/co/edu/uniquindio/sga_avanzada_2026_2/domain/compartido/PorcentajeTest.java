package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PorcentajeTest {

    @Test
    @Tag("PORC-01")
    void deberiaAceptarValoresEntreCeroYCien() {
        assertEquals(0, new Porcentaje(0).valor());
        assertEquals(100, new Porcentaje(100).valor());
    }

    @Test
    @Tag("PORC-01")
    void deberiaRechazarValoresFueraDeRango() {
        assertThrows(ReglaDominioException.class, () -> new Porcentaje(-1));
        assertThrows(ReglaDominioException.class, () -> new Porcentaje(101));
    }
}
