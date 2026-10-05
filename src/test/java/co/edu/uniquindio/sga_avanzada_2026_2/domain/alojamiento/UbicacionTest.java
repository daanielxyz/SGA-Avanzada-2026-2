package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UbicacionTest {

    @Test
    @Tag("UBI-01")
    void deberiaAceptarCoordenadasEnRango() {
        Ubicacion ubicacion = new Ubicacion(4.5339, -75.6811);

        assertEquals(4.5339, ubicacion.latitud());
        assertEquals(-75.6811, ubicacion.longitud());
        new Ubicacion(-90, 180);
    }

    @Test
    @Tag("UBI-01")
    void deberiaRechazarCoordenadasFueraDeRango() {
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(90.1, 0));
        assertThrows(ReglaDominioException.class, () -> new Ubicacion(0, -180.1));
    }
}
