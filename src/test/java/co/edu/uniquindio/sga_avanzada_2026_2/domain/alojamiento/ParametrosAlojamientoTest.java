package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParametrosAlojamientoTest {

    private static ParametrosAlojamiento parametros(LocalTime horaEntrada, Duration preparacion, Duration plazo,
                                                    int minimoMedios) {
        return new ParametrosAlojamiento(12, horaEntrada, LocalTime.of(11, 0), preparacion, plazo,
                LocalTime.of(22, 0), new Porcentaje(30), minimoMedios, 1, 2);
    }

    @Test
    @Tag("ALO-03")
    void deberiaCrearParametrosValidos() {
        ParametrosAlojamiento parametros = parametros(LocalTime.of(15, 0), Duration.ofHours(3), Duration.ofHours(24), 2);

        assertEquals(Duration.ofHours(3), parametros.tiempoPreparacion());
        assertEquals(new Porcentaje(30), parametros.anticipo());
    }

    @Test
    @Tag("ALO-02")
    void deberiaExigirLasHorasDelAlojamiento() {
        assertThrows(ReglaDominioException.class,
                () -> parametros(null, Duration.ofHours(3), Duration.ofHours(24), 2));
    }

    @Test
    @Tag("TPRE-01")
    void deberiaAceptarTiempoDePreparacionCero() {
        assertEquals(Duration.ZERO, parametros(LocalTime.of(15, 0), Duration.ZERO, Duration.ofHours(24), 2)
                .tiempoPreparacion());
    }

    @Test
    @Tag("TPRE-01")
    void deberiaRechazarTiempoDePreparacionNegativoOFraccionario() {
        assertThrows(ReglaDominioException.class,
                () -> parametros(LocalTime.of(15, 0), Duration.ofHours(-1), Duration.ofHours(24), 2));
        assertThrows(ReglaDominioException.class,
                () -> parametros(LocalTime.of(15, 0), Duration.ofMinutes(90), Duration.ofHours(24), 2));
    }

    @Test
    @Tag("ALO-03")
    void deberiaRechazarPlazoDeConfirmacionNoPositivo() {
        assertThrows(ReglaDominioException.class,
                () -> parametros(LocalTime.of(15, 0), Duration.ofHours(3), Duration.ZERO, 2));
    }

    @Test
    @Tag("ALO-06")
    void deberiaRechazarMinimosNegativos() {
        assertThrows(ReglaDominioException.class,
                () -> parametros(LocalTime.of(15, 0), Duration.ofHours(3), Duration.ofHours(24), -1));
    }

    @Test
    @Tag("TEM-04")
    void deberiaRechazarMinimoDeTemporadasNegativo() {
        assertThrows(ReglaDominioException.class, () -> new ParametrosAlojamiento(12, LocalTime.of(15, 0),
                LocalTime.of(11, 0), Duration.ofHours(3), Duration.ofHours(24), LocalTime.of(22, 0),
                new Porcentaje(30), 2, 1, -1));
    }
}
