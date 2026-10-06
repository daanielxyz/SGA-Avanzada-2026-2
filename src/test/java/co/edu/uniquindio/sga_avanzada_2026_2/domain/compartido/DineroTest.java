package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DineroTest {

    @Test
    @Tag("DIN-01")
    void deberiaRedondearAlPesoMasCercano() {
        assertEquals(Dinero.de(1501), new Dinero(new BigDecimal("1500.5")));
        assertEquals(Dinero.de(1500), new Dinero(new BigDecimal("1500.4")));
    }

    @Test
    @Tag("DIN-01")
    void deberiaRechazarMontoNulo() {
        assertThrows(ReglaDominioException.class, () -> new Dinero(null));
    }

    @Test
    @Tag("DIN-02")
    void deberiaOperarSinModificarLosOperandos() {
        Dinero cien = Dinero.de(100_000);
        Dinero treinta = Dinero.de(30_000);

        assertEquals(Dinero.de(130_000), cien.sumar(treinta));
        assertEquals(Dinero.de(70_000), cien.restar(treinta));
        assertEquals(Dinero.de(300_000), cien.multiplicar(3));
        assertEquals(Dinero.de(100_000), cien);
        assertEquals(Dinero.de(30_000), treinta);
    }

    @Test
    @Tag("DIN-03")
    void deberiaRechazarMontoNegativo() {
        assertThrows(ReglaDominioException.class, () -> Dinero.de(-1));
        assertThrows(ReglaDominioException.class, () -> new Dinero(new BigDecimal("-0.6")));
    }

    @Test
    @Tag("DIN-03")
    void deberiaRechazarRestaConResultadoNegativo() {
        assertThrows(ReglaDominioException.class, () -> Dinero.de(50_000).restar(Dinero.de(80_000)));
    }

    @Test
    @Tag("DIN-02")
    @Tag("RES-15")
    void deberiaCalcularUnPorcentajeRedondeandoUnaSolaVez() {
        assertEquals(Dinero.de(120_000), Dinero.de(400_000).porcentaje(new Porcentaje(30)));
        assertEquals(Dinero.de(5), Dinero.de(15).porcentaje(new Porcentaje(30)));  // 4,5 → 5
        assertEquals(Dinero.CERO, Dinero.de(400_000).porcentaje(new Porcentaje(0)));
        assertTrue(Dinero.de(119_999).esMenorQue(Dinero.de(120_000)));
    }

    @Test
    @Tag("DIN-03")
    void deberiaPermitirRestaHastaCero() {
        Dinero resultado = Dinero.de(50_000).restar(Dinero.de(50_000));

        assertTrue(resultado.esCero());
        assertTrue(Dinero.de(1).esPositivo());
    }
}
