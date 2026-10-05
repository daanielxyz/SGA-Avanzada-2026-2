package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Temporada;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TarificacionDomainServiceTest {

    private static final int UMBRAL = 12;
    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final TemporadaId BASE = new TemporadaId("TEM-BASE");
    private static final TemporadaId ALTA = new TemporadaId("TEM-ALTA");
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);

    private final TarificacionDomainService servicio = new TarificacionDomainService();

    /** Base todo el año y Alta del 15 al 31 de diciembre. */
    private static final CalendarioTemporadas CALENDARIO = new CalendarioTemporadas(new AlojamientoId("ALO-1"),
            List.of(new Temporada(BASE, "Base", null, null, true, 0, true),
                    new Temporada(ALTA, "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 31), false, 0, true)));

    private static Tarifa tarifa(String id, TemporadaId temporada, long valor, int version) {
        return new Tarifa(new TarifaId(id), APT, temporada, Dinero.de(valor), version, DESDE);
    }

    private static final List<Tarifa> TARIFAS = List.of(tarifa("T-BASE", BASE, 100_000, 1),
            tarifa("T-ALTA", ALTA, 150_000, 1));

    private static Ocupante ocupante(String id, int edadALaEntrada, LocalDate entrada) {
        return new Ocupante(new OcupanteId(id), "Ocupante " + id, entrada.minusYears(edadALaEntrada), null, null);
    }

    @Test
    @Tag("RN-05")
    @Tag("TAR-04")
    @Tag("NOC-04")
    void deberiaCalcularTotalCotizacionCruzandoTemporadasDiferentes() {
        // 13 y 14 de diciembre en Base, 15 en Alta; 2 adultos
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 16));
        List<Ocupante> grupo = List.of(ocupante("A", 30, estancia.entrada()), ocupante("B", 28, estancia.entrada()));

        Cotizacion cotizacion = servicio.calcularValorEstancia(APT, estancia, grupo, UMBRAL, CALENDARIO, TARIFAS);

        assertEquals(Dinero.de(2 * 100_000 + 2 * 100_000 + 2 * 150_000), cotizacion.total());
        assertEquals(3, cotizacion.desglose().size());
        LineaCotizacion alta = cotizacion.desglose().get(2);
        assertEquals(new Noche(LocalDate.of(2026, 12, 15)), alta.noche());
        assertEquals(ALTA, alta.temporadaId());
        assertEquals(Dinero.de(150_000), alta.tarifa());
        assertEquals(2, alta.ocupantesFacturables());
        assertEquals(Dinero.de(300_000), alta.subtotal());
    }

    @Test
    @Tag("RN-05")
    @Tag("TAR-04")
    void deberiaRechazarCotizacionSiFaltaTarifaEnUnaTemporada() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 16));
        List<Tarifa> soloBase = List.of(tarifa("T-BASE", BASE, 100_000, 1));

        assertThrows(ReglaDominioException.class, () -> servicio.calcularValorEstancia(APT, estancia,
                List.of(ocupante("A", 30, estancia.entrada())), UMBRAL, CALENDARIO, soloBase));
    }

    @Test
    @Tag("RN-06")
    @Tag("OCU-09")
    void losNoFacturablesOcupanCupoPeroNoPagan() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3));
        List<Ocupante> familia = List.of(ocupante("A", 35, estancia.entrada()), ocupante("N", 8, estancia.entrada()));

        Cotizacion cotizacion = servicio.calcularValorEstancia(APT, estancia, familia, UMBRAL, CALENDARIO, TARIFAS);

        assertEquals(Dinero.de(2 * 100_000), cotizacion.total());
        assertEquals(1, cotizacion.desglose().getFirst().ocupantesFacturables());
    }

    @Test
    @Tag("TAR-06")
    void deberiaUsarLaVersionMasRecienteDeLaTarifa() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        List<Tarifa> conVersiones = List.of(tarifa("T-BASE-1", BASE, 100_000, 1), tarifa("T-BASE-2", BASE, 110_000, 2));

        Cotizacion cotizacion = servicio.calcularValorEstancia(APT, estancia,
                List.of(ocupante("A", 30, estancia.entrada())), UMBRAL, CALENDARIO, conVersiones);

        assertEquals(Dinero.de(110_000), cotizacion.total());
    }

    @Test
    @Tag("TAR-01")
    void deberiaIgnorarTarifasDeOtrosApartamentos() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        List<Tarifa> deOtro = List.of(new Tarifa(new TarifaId("X"), new ApartamentoId("APT-102"), BASE,
                Dinero.de(90_000), 1, DESDE));

        assertThrows(ReglaDominioException.class, () -> servicio.calcularValorEstancia(APT, estancia,
                List.of(ocupante("A", 30, estancia.entrada())), UMBRAL, CALENDARIO, deOtro));
    }

    @Test
    @Tag("COT-03")
    void deberiaSerDeterminista() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 18));
        List<Ocupante> grupo = List.of(ocupante("A", 30, estancia.entrada()));

        assertEquals(servicio.calcularValorEstancia(APT, estancia, grupo, UMBRAL, CALENDARIO, TARIFAS),
                servicio.calcularValorEstancia(APT, estancia, grupo, UMBRAL, CALENDARIO, TARIFAS));
    }
}
