package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TarifaTest {

    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");
    private static final LocalDate ENERO = LocalDate.of(2026, 1, 1);

    private static Tarifa tarifa(Dinero valor) {
        return new Tarifa(new TarifaId("TAR-1"), APT, ALTA, valor, 1, ENERO);
    }

    @Test
    @Tag("TAR-01")
    void deberiaCrearTarifaParaApartamentoYTemporada() {
        Tarifa tarifa = tarifa(Dinero.de(120_000));

        assertTrue(tarifa.aplicaA(APT, ALTA));
        assertFalse(tarifa.aplicaA(new ApartamentoId("APT-102"), ALTA));
    }

    @Test
    @Tag("TAR-01")
    void deberiaExigirApartamentoTemporadaYValor() {
        assertThrows(ReglaDominioException.class,
                () -> new Tarifa(new TarifaId("TAR-1"), null, ALTA, Dinero.de(1), 1, ENERO));
        assertThrows(ReglaDominioException.class,
                () -> new Tarifa(new TarifaId("TAR-1"), APT, null, Dinero.de(1), 1, ENERO));
    }

    @Test
    @Tag("TAR-02")
    void deberiaRechazarValorCero() {
        assertThrows(ReglaDominioException.class, () -> tarifa(Dinero.CERO));
    }

    @Test
    @Tag("TAR-05")
    void nuevaVersionDeberiaConservarLaAnterior() {
        Tarifa v1 = tarifa(Dinero.de(120_000));

        Tarifa v2 = v1.nuevaVersion(new TarifaId("TAR-2"), Dinero.de(150_000), LocalDate.of(2026, 6, 1));

        assertEquals(2, v2.version());
        assertEquals(Dinero.de(150_000), v2.valorPorOcupante());
        assertTrue(v2.aplicaA(APT, ALTA));
        assertEquals(1, v1.version());
        assertEquals(Dinero.de(120_000), v1.valorPorOcupante());
    }

    @Test
    @Tag("TAR-05")
    void deberiaRechazarNuevaVersionQueRijaAntesQueLaActual() {
        Tarifa v1 = tarifa(Dinero.de(120_000));

        assertThrows(ReglaDominioException.class,
                () -> v1.nuevaVersion(new TarifaId("TAR-2"), Dinero.de(150_000), ENERO.minusDays(1)));
    }

    @Test
    @Tag("TAR-05")
    void laPrimeraTarifaNaceEnLaVersionUno() {
        Tarifa tarifa = Tarifa.crear(new TarifaId("TAR-1"), APT, ALTA, Dinero.de(120_000), ENERO);

        assertEquals(1, tarifa.version());
        assertEquals(ENERO, tarifa.vigenteDesde());
    }

    @Test
    @Tag("TAR-02")
    void laPrimeraTarifaTambienExigeValorPositivo() {
        assertThrows(ReglaDominioException.class,
                () -> Tarifa.crear(new TarifaId("TAR-1"), APT, ALTA, Dinero.CERO, ENERO));
    }
}
