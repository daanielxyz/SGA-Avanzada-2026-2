package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Versiones de tarifa contra H2 con el esquema de Flyway.
 */
@DataJpaTest
@Import(TarifaRepositoryJpa.class)
class TarifaRepositoryJpaTest {

    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final TemporadaId BASE = new TemporadaId("TEM-BASE");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");

    @Autowired
    private TarifaRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaDevolverLaUltimaVersionDeCadaTemporada() {
        Tarifa baseV1 = new Tarifa(new TarifaId("T1"), APT, BASE, Dinero.de(100_000), 1, LocalDate.of(2026, 1, 1));
        Tarifa baseV2 = baseV1.nuevaVersion(new TarifaId("T2"), Dinero.de(110_000), LocalDate.of(2026, 6, 1));
        Tarifa altaV1 = new Tarifa(new TarifaId("T3"), APT, ALTA, Dinero.de(150_000), 1, LocalDate.of(2026, 1, 1));
        Tarifa deOtroApto = new Tarifa(new TarifaId("T4"), new ApartamentoId("APT-102"), BASE, Dinero.de(90_000), 1,
                LocalDate.of(2026, 1, 1));
        List.of(baseV1, baseV2, altaV1, deOtroApto).forEach(repositorio::guardar);
        sincronizar();

        List<Tarifa> vigentes = repositorio.buscarVigentesPorApartamento(APT, LocalDate.of(2026, 7, 1)).stream()
                .sorted(Comparator.comparing(t -> t.temporadaId().valor()))
                .toList();

        assertEquals(2, vigentes.size());
        Tarifa alta = vigentes.get(0);
        assertEquals(ALTA, alta.temporadaId());
        assertEquals(Dinero.de(150_000), alta.valorPorOcupante());
        Tarifa base = vigentes.get(1);
        assertEquals(BASE, base.temporadaId());
        assertEquals(2, base.version());
        assertEquals(Dinero.de(110_000), base.valorPorOcupante());
        assertEquals(LocalDate.of(2026, 6, 1), base.vigenteDesde());
    }

    @Test
    void unaVersionConVigenciaFuturaNoDeberiaAplicarTodavia() {
        Tarifa v1 = new Tarifa(new TarifaId("T1"), APT, BASE, Dinero.de(100_000), 1, LocalDate.of(2026, 1, 1));
        Tarifa v2 = v1.nuevaVersion(new TarifaId("T2"), Dinero.de(130_000), LocalDate.of(2026, 6, 1));
        repositorio.guardar(v1);
        repositorio.guardar(v2);
        sincronizar();

        Tarifa antesDeJunio = repositorio.buscarVigentesPorApartamento(APT, LocalDate.of(2026, 5, 31)).getFirst();
        Tarifa desdeJunio = repositorio.buscarVigentesPorApartamento(APT, LocalDate.of(2026, 6, 1)).getFirst();

        assertEquals(Dinero.de(100_000), antesDeJunio.valorPorOcupante());
        assertEquals(Dinero.de(130_000), desdeJunio.valorPorOcupante());
        assertTrue(repositorio.buscarVigentesPorApartamento(APT, LocalDate.of(2025, 12, 31)).isEmpty());
    }

    @Test
    void noDeberiaPermitirDosTarifasConLaMismaVersion() {
        repositorio.guardar(new Tarifa(new TarifaId("T1"), APT, BASE, Dinero.de(100_000), 1, LocalDate.of(2026, 1, 1)));
        repositorio.guardar(new Tarifa(new TarifaId("T9"), APT, BASE, Dinero.de(120_000), 1, LocalDate.of(2026, 2, 1)));

        assertThrows(RuntimeException.class, this::sincronizar);
    }

    @Test
    void deberiaDevolverVacioSiElApartamentoNoTieneTarifas() {
        assertTrue(repositorio.buscarVigentesPorApartamento(new ApartamentoId("APT-999"), LocalDate.of(2026, 1, 1))
                .isEmpty());
    }
}
