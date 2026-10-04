package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstanciaTest {

    private static Estancia estancia(int diaEntrada, int diaSalida) {
        return new Estancia(LocalDate.of(2026, 10, diaEntrada), LocalDate.of(2026, 10, diaSalida));
    }

    @Test
    @Tag("RN-03")
    void deberiaCrearEstanciaConSalidaPosteriorAEntrada() {
        Estancia estancia = estancia(10, 12);

        assertEquals(LocalDate.of(2026, 10, 10), estancia.entrada());
        assertEquals(LocalDate.of(2026, 10, 12), estancia.salida());
    }

    @Test
    @Tag("RN-03")
    void deberiaRechazarEstanciaConSalidaNoPosteriorAEntrada() {
        assertThrows(ReglaDominioException.class, () -> estancia(10, 10));
        assertThrows(ReglaDominioException.class, () -> estancia(12, 10));
    }

    @Test
    @Tag("RN-03")
    void deberiaRechazarEstanciaSinFechas() {
        assertThrows(ReglaDominioException.class, () -> new Estancia(null, LocalDate.of(2026, 10, 12)));
        assertThrows(ReglaDominioException.class, () -> new Estancia(LocalDate.of(2026, 10, 10), null));
    }

    @Test
    @Tag("EST-04")
    void deberiaContarDosNochesDelDiezAlDoce() {
        assertEquals(2, estancia(10, 12).cantidadNoches());
    }

    @Test
    @Tag("NOC-02")
    void deberiaExcluirLaNocheDeSalida() {
        List<Noche> noches = estancia(10, 12).noches();

        assertEquals(List.of(new Noche(LocalDate.of(2026, 10, 10)), new Noche(LocalDate.of(2026, 10, 11))), noches);
    }

    @Test
    @Tag("RN-01")
    @Tag("EST-03")
    void deberiaDetectarSolapeCuandoCompartenNoches() {
        assertTrue(estancia(10, 13).seSolapaCon(estancia(12, 15)));
        assertTrue(estancia(12, 15).seSolapaCon(estancia(10, 13)));
        assertTrue(estancia(10, 20).seSolapaCon(estancia(12, 14)));
        assertTrue(estancia(10, 12).seSolapaCon(estancia(10, 12)));
    }

    @Test
    @Tag("RN-01")
    @Tag("EST-03")
    void noDeberiaSolaparEstanciasContiguasNiSeparadas() {
        assertFalse(estancia(10, 12).seSolapaCon(estancia(12, 14)));
        assertFalse(estancia(12, 14).seSolapaCon(estancia(10, 12)));
        assertFalse(estancia(10, 12).seSolapaCon(estancia(15, 18)));
    }
}
