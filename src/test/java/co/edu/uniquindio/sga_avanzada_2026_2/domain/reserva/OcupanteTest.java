package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcupanteTest {

    private static final int UMBRAL = 12;
    private static final LocalDate ENTRADA = LocalDate.of(2026, 12, 20);

    private static Ocupante nacidoEl(LocalDate fechaNacimiento) {
        return new Ocupante(new OcupanteId("OCU-1"), "Ana", fechaNacimiento, null, null);
    }

    @Test
    @Tag("RN-06")
    @Tag("OCU-03")
    void deberiaSerFacturableSiAlcanzaElUmbralEnLaFechaDeEntrada() {
        assertTrue(nacidoEl(ENTRADA.minusYears(UMBRAL)).esFacturableA(ENTRADA, UMBRAL));
        assertTrue(nacidoEl(ENTRADA.minusYears(40)).esFacturableA(ENTRADA, UMBRAL));
    }

    @Test
    @Tag("RN-06")
    @Tag("OCU-03")
    void deberiaNoSerFacturableSiTieneMenosDelUmbralEnLaFechaDeEntrada() {
        assertFalse(nacidoEl(ENTRADA.minusYears(UMBRAL).plusDays(1)).esFacturableA(ENTRADA, UMBRAL));
    }

    @Test
    @Tag("OCU-03")
    void cumplirAniosDuranteLaEstanciaNoLoVuelveFacturable() {
        Ocupante cumpleDosDiasDespues = nacidoEl(ENTRADA.plusDays(2).minusYears(UMBRAL));

        assertFalse(cumpleDosDiasDespues.esFacturableA(ENTRADA, UMBRAL));
        assertEquals(UMBRAL, cumpleDosDiasDespues.edadA(ENTRADA.plusDays(2)));
    }

    @Test
    @Tag("OCU-13")
    void deberiaCalcularLosAniosCumplidos() {
        Ocupante ocupante = nacidoEl(LocalDate.of(2000, 5, 10));

        assertEquals(25, ocupante.edadA(LocalDate.of(2026, 5, 9)));
        assertEquals(26, ocupante.edadA(LocalDate.of(2026, 5, 10)));
    }

    @Test
    @Tag("OCU-07")
    void deberiaExigirNombreYFechaDeNacimiento() {
        assertThrows(ReglaDominioException.class,
                () -> new Ocupante(new OcupanteId("OCU-1"), " ", LocalDate.of(2000, 1, 1), null, null));
        assertThrows(ReglaDominioException.class, () -> new Ocupante(new OcupanteId("OCU-1"), "Ana", null, null, null));
    }
}
