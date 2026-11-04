package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("RP-03")
class MascotasTest {

    private static ParametrosAlojamiento parametros(boolean acepta, int limite, long cargo) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2, 2, 2,
                ModoPagoEnLinea.ANTICIPO, Duration.ofMinutes(30), 8, acepta, limite, Dinero.de(cargo));
    }

    @Test
    void deberiaAceptarMascotasHastaElLimite() {
        assertDoesNotThrow(() -> parametros(true, 2, 30_000).exigirMascotas(2));
    }

    @Test
    void deberiaRechazarMasMascotasQueElLimite() {
        assertThrows(ReglaDominioException.class, () -> parametros(true, 2, 30_000).exigirMascotas(3));
    }

    @Test
    void deberiaRechazarMascotasSiNoSeAceptan() {
        assertThrows(ReglaDominioException.class, () -> parametros(false, 0, 0).exigirMascotas(1));
    }

    @Test
    void sinMascotasNuncaHayProblema() {
        assertDoesNotThrow(() -> parametros(false, 0, 0).exigirMascotas(0));
    }

    @Test
    void cadaMascotaGeneraSuCargo() {
        assertEquals(Optional.of(Dinero.de(60_000)), parametros(true, 2, 30_000).cargoPorMascotas(2));
        assertEquals(Optional.empty(), parametros(true, 2, 30_000).cargoPorMascotas(0));
    }

    @Test
    void elCupoYElLimiteNoPuedenSerNegativos() {
        assertThrows(ReglaDominioException.class, () -> parametros(true, -1, 0));
    }
}
