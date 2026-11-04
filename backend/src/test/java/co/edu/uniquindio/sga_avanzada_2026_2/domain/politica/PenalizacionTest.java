package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PenalizacionTest {

    private static final Dinero TOTAL = Dinero.de(400_000);
    private static final Dinero PAGADO = Dinero.de(50_000);
    private static final Dinero ANTICIPO = Dinero.de(120_000);

    @Test
    @Tag("POL-03")
    @Tag("DEC-41")
    void deberiaCalcularElPorcentajeSobreLaBaseElegida() {
        Porcentaje mitad = new Porcentaje(50);

        assertEquals(Dinero.de(200_000), Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, mitad)
                .calcular(TOTAL, PAGADO, ANTICIPO));
        assertEquals(Dinero.de(25_000), Penalizacion.porcentaje(BaseRetencion.PAGADO, mitad)
                .calcular(TOTAL, PAGADO, ANTICIPO));
        assertEquals(Dinero.de(60_000), Penalizacion.porcentaje(BaseRetencion.ANTICIPO_EXIGIDO, mitad)
                .calcular(TOTAL, PAGADO, ANTICIPO));
    }

    @Test
    @Tag("DEC-41")
    void unMontoFijoNuncaSuperaLaBase() {
        Penalizacion ochentaMil = Penalizacion.montoFijo(BaseRetencion.PAGADO, Dinero.de(80_000));

        assertEquals(Dinero.de(50_000), ochentaMil.calcular(TOTAL, PAGADO, ANTICIPO));
        assertEquals(Dinero.de(80_000), ochentaMil.calcular(TOTAL, Dinero.de(90_000), ANTICIPO));
        assertEquals(Dinero.CERO, ochentaMil.calcular(TOTAL, Dinero.CERO, ANTICIPO));
    }

    @Test
    @Tag("DEC-41")
    void deberiaSerPorcentajeOMontoFijoPeroNoAmbos() {
        assertThrows(ReglaDominioException.class,
                () -> new Penalizacion(BaseRetencion.VALOR_TOTAL, new Porcentaje(10), Dinero.de(1_000)));
        assertThrows(ReglaDominioException.class, () -> new Penalizacion(BaseRetencion.VALOR_TOTAL, null, null));
        assertThrows(ReglaDominioException.class, () -> Penalizacion.porcentaje(null, new Porcentaje(10)));
    }
}
