package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
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
        assertEquals(Dinero.de(60_000), parametros(true, 2, 30_000).calcularExtras(0, 0, 2, 3).mascotas());
        assertEquals(Dinero.CERO, parametros(true, 2, 30_000).calcularExtras(0, 0, 0, 3).mascotas());
    }

    private static Apartamento apartamento(Integer limiteMascotas) {
        return new Apartamento(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"), "Suite", null,
                new Capacidad(4), new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(),
                List.of(new Caracteristica("Wifi")), List.of(), true, limiteMascotas);
    }

    @Test
    @Tag("DEC-93")
    void sinLimitePropioElApartamentoSigueElDelAlojamiento() {
        assertEquals(Optional.empty(), apartamento(null).limiteMascotas());
        assertEquals(2, parametros(true, 2, 30_000).limiteMascotasPara(apartamento(null)));
        assertDoesNotThrow(() -> parametros(true, 2, 30_000).exigirMascotas(2, apartamento(null)));
        assertThrows(ReglaDominioException.class,
                () -> parametros(true, 2, 30_000).exigirMascotas(3, apartamento(null)));
    }

    @Test
    @Tag("DEC-93")
    void elLimitePropioDelApartamentoReemplazaAlDelAlojamiento() {
        assertEquals(0, parametros(true, 2, 30_000).limiteMascotasPara(apartamento(0)));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> parametros(true, 2, 30_000).exigirMascotas(1, apartamento(0)));
        assertEquals("El apartamento APT-101 admite máximo 0 mascotas por reserva", error.getMessage());
        assertDoesNotThrow(() -> parametros(true, 2, 30_000).exigirMascotas(4, apartamento(4)));
    }

    @Test
    @Tag("DEC-93")
    void siElAlojamientoNoAceptaMascotasNingunApartamentoLasRecibe() {
        assertEquals(0, parametros(false, 0, 0).limiteMascotasPara(apartamento(3)));
        assertThrows(ReglaDominioException.class, () -> parametros(false, 0, 0).exigirMascotas(1, apartamento(3)));
    }

    @Test
    @Tag("DEC-93")
    void elLimiteDelApartamentoNoPuedeSerNegativoYSePuedeQuitar() {
        assertThrows(ReglaDominioException.class, () -> apartamento(-1));
        Apartamento conLimite = apartamento(1);
        assertThrows(ReglaDominioException.class, () -> conLimite.fijarLimiteMascotas(-1));
        conLimite.fijarLimiteMascotas(null);
        assertEquals(Optional.empty(), conLimite.limiteMascotas());
    }

    @Test
    void elCupoYElLimiteNoPuedenSerNegativos() {
        assertThrows(ReglaDominioException.class, () -> parametros(true, -1, 0));
    }
}
