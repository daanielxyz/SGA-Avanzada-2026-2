package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporadaTest {

    private static LocalDate dic(int d) {
        return LocalDate.of(2026, 12, d);
    }

    private static Temporada especifica(String id, LocalDate inicio, LocalDate fin) {
        return new Temporada(new TemporadaId(id), "Alta " + id, inicio, fin, false, 0, true);
    }

    private static Temporada base() {
        return new Temporada(new TemporadaId("TEM-BASE"), "Base", null, null, true, 0, true);
    }

    @Test
    @Tag("TEM-01")
    void deberiaAceptarRangoDeUnSoloDia() {
        assertTrue(especifica("T1", dic(24), dic(24)).cubre(new Noche(dic(24))));
    }

    @Test
    @Tag("TEM-01")
    void deberiaRechazarRangoConFinAnteriorAlInicio() {
        assertThrows(ReglaDominioException.class, () -> especifica("T1", dic(20), dic(10)));
        assertThrows(ReglaDominioException.class, () -> especifica("T1", null, dic(10)));
    }

    @Test
    @Tag("TEM-01")
    void deberiaCubrirAmbosExtremosDelRango() {
        Temporada navidad = especifica("T1", dic(15), dic(31));

        assertTrue(navidad.cubre(new Noche(dic(15))));
        assertTrue(navidad.cubre(new Noche(dic(31))));
        assertFalse(navidad.cubre(new Noche(dic(14))));
        assertFalse(navidad.cubre(new Noche(LocalDate.of(2027, 1, 1))));
    }

    @Test
    @Tag("TEM-09")
    void laBaseNoDeberiaTenerRangoNiEstanciaMinima() {
        assertThrows(ReglaDominioException.class,
                () -> new Temporada(new TemporadaId("B"), "Base", dic(1), dic(5), true, 0, true));
        assertThrows(ReglaDominioException.class,
                () -> new Temporada(new TemporadaId("B"), "Base", null, null, true, 2, true));
        assertFalse(base().cubre(new Noche(dic(1))));
    }

    @Test
    @Tag("TEM-07")
    void deberiaRechazarEstanciaMinimaNegativa() {
        assertThrows(ReglaDominioException.class,
                () -> new Temporada(new TemporadaId("T1"), "Alta", dic(1), dic(5), false, -1, true));
    }

    @Test
    @Tag("TEM-02")
    void deberiaDetectarSolapeAunqueSoloCompartanUnExtremo() {
        assertTrue(especifica("T1", dic(1), dic(10)).seSolapaCon(especifica("T2", dic(10), dic(20))));
        assertFalse(especifica("T1", dic(1), dic(10)).seSolapaCon(especifica("T2", dic(11), dic(20))));
        assertFalse(especifica("T1", dic(1), dic(10)).seSolapaCon(base()));
    }

    @Test
    @Tag("TEM-10")
    void laBaseNoDeberiaPoderDesactivarse() {
        assertThrows(ReglaDominioException.class, () -> base().desactivar());
        assertThrows(ReglaDominioException.class,
                () -> new Temporada(new TemporadaId("B"), "Base", null, null, true, 0, false));
    }

    @Test
    @Tag("TEM-06")
    void unaTemporadaInactivaNoCubreNiSeSolapa() {
        Temporada alta = especifica("T1", dic(1), dic(10));

        alta.desactivar();

        assertFalse(alta.cubre(new Noche(dic(5))));
        assertFalse(alta.seSolapaCon(especifica("T2", dic(5), dic(6))));
        assertThrows(ReglaDominioException.class, alta::desactivar);
    }
}
