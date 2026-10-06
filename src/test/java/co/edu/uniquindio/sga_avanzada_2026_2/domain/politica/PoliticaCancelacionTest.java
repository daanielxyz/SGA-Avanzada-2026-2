package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaCancelacionTest {

    private static final AlojamientoId ALOJAMIENTO = new AlojamientoId("ALO-1");
    private static final int MINIMO_FICHA = 2;
    private static final Penalizacion TODO = porcentaje(100);
    private static final Penalizacion MITAD = porcentaje(50);
    private static final Penalizacion NADA = porcentaje(0);

    private static Penalizacion porcentaje(int valor) {
        return Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(valor));
    }

    /** Menos de 24 h: 100 % · de 24 a 72 h: 50 % · 72 h o más: gratis. Se pasan desordenados a propósito. */
    private static List<TramoCancelacion> tramos() {
        return List.of(new TramoCancelacion(72, NADA), new TramoCancelacion(0, TODO), new TramoCancelacion(24, MITAD));
    }

    private static PoliticaCancelacion politica() {
        return PoliticaCancelacion.crear(new PoliticaId("POL-1"), ALOJAMIENTO, tramos(), TODO, MINIMO_FICHA);
    }

    @Test
    @Tag("POL-02")
    @Tag("TRAM-01")
    void deberiaAplicarElTramoDeMayorAntelacionAlcanzada() {
        PoliticaCancelacion politica = politica();

        assertEquals(NADA, politica.penalizacionPara(100));
        assertEquals(NADA, politica.penalizacionPara(72));
        assertEquals(MITAD, politica.penalizacionPara(71));
        assertEquals(MITAD, politica.penalizacionPara(24));
        assertEquals(TODO, politica.penalizacionPara(23));
        assertEquals(TODO, politica.penalizacionPara(-5)); // ya pasó la hora de entrada
    }

    @Test
    @Tag("POL-01")
    void deberiaExigirElMinimoDeTramosConfigurado() {
        List<TramoCancelacion> unTramo = List.of(new TramoCancelacion(0, TODO));

        assertThrows(ReglaDominioException.class, () -> PoliticaCancelacion.crear(new PoliticaId("POL-1"),
                ALOJAMIENTO, unTramo, TODO, MINIMO_FICHA));
        assertEquals(1, PoliticaCancelacion.crear(new PoliticaId("POL-1"), ALOJAMIENTO, unTramo, TODO, 1).version());
    }

    @Test
    @Tag("POL-02")
    void losTramosDebenCubrirDesdeCeroHorasSinRepetirse() {
        assertThrows(ReglaDominioException.class, () -> PoliticaCancelacion.crear(new PoliticaId("POL-1"),
                ALOJAMIENTO, List.of(new TramoCancelacion(24, MITAD), new TramoCancelacion(72, NADA)), TODO,
                MINIMO_FICHA));
        assertThrows(ReglaDominioException.class, () -> PoliticaCancelacion.crear(new PoliticaId("POL-1"),
                ALOJAMIENTO, List.of(new TramoCancelacion(0, TODO), new TramoCancelacion(0, MITAD)), TODO,
                MINIMO_FICHA));
    }

    @Test
    @Tag("POL-04")
    void unaNuevaVersionNoModificaLaAnterior() {
        PoliticaCancelacion v1 = politica();

        PoliticaCancelacion v2 = v1.nuevaVersion(new PoliticaId("POL-2"),
                List.of(new TramoCancelacion(0, MITAD), new TramoCancelacion(48, NADA)), MITAD, MINIMO_FICHA);

        assertEquals(2, v2.version());
        assertEquals(ALOJAMIENTO, v2.alojamientoId());
        assertEquals(1, v1.version());
        assertEquals(TODO, v1.penalizacionPara(0));
        assertEquals(MITAD, v2.penalizacionPara(0));
        assertNotEquals(v1, v2);
    }

    @Test
    @Tag("POL-06")
    void deberiaDefinirLaConsecuenciaDelNoShow() {
        assertEquals(TODO, politica().penalizacionNoShow());
        assertThrows(ReglaDominioException.class, () -> PoliticaCancelacion.crear(new PoliticaId("POL-1"),
                ALOJAMIENTO, tramos(), null, MINIMO_FICHA));
    }
}
