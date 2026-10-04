package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloqueoTest {

    private static final BloqueoId ID = new BloqueoId("BLO-1");

    private static LocalDate dia(int d) {
        return LocalDate.of(2026, 10, d);
    }

    private static Noche noche(int d) {
        return new Noche(dia(d));
    }

    @Test
    @Tag("BLO-02")
    void deberiaRechazarBloqueoConFinNoPosteriorAlInicio() {
        assertThrows(ReglaDominioException.class, () -> new Bloqueo(ID, dia(10), dia(10), "Pintura", true));
        assertThrows(ReglaDominioException.class, () -> new Bloqueo(ID, dia(12), dia(10), "Pintura", true));
    }

    @Test
    @Tag("BLO-02")
    void deberiaRechazarBloqueoSinMotivo() {
        assertThrows(ReglaDominioException.class, () -> new Bloqueo(ID, dia(10), dia(12), " ", true));
    }

    @Test
    @Tag("RN-07")
    @Tag("BLO-03")
    void deberiaCubrirDesdeElInicioHastaLaVisperaDelFin() {
        Bloqueo bloqueo = new Bloqueo(ID, dia(10), dia(12), "Pintura", true);

        assertTrue(bloqueo.cubre(noche(10)));
        assertTrue(bloqueo.cubre(noche(11)));
    }

    @Test
    @Tag("RN-07")
    @Tag("BLO-03")
    void noDeberiaCubrirNochesFueraDelRango() {
        Bloqueo bloqueo = new Bloqueo(ID, dia(10), dia(12), "Pintura", true);

        assertFalse(bloqueo.cubre(noche(9)));
        assertFalse(bloqueo.cubre(noche(12)));
    }

    @Test
    @Tag("BLO-06")
    void deberiaLiberarNochesAlLevantarseYConservarElRegistro() {
        Bloqueo bloqueo = new Bloqueo(ID, dia(10), dia(12), "Pintura", true);

        bloqueo.levantar();

        assertFalse(bloqueo.vigente());
        assertFalse(bloqueo.cubre(noche(10)));
    }

    @Test
    @Tag("BLO-06")
    void deberiaRechazarLevantarUnBloqueoYaLevantado() {
        Bloqueo bloqueo = new Bloqueo(ID, dia(10), dia(12), "Pintura", false);

        assertThrows(ReglaDominioException.class, bloqueo::levantar);
    }
}
