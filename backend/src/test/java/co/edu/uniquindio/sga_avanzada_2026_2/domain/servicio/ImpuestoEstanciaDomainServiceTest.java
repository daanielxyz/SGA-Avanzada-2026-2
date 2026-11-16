package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.IvaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El IVA de la estancia desde la reserva (DEC-134): las tarifas son la base, se congela al crear con el documento
 * del titular, se revisa en la llegada y va en su propio cargo, aparte del alojamiento.
 */
@Tag("DEC-134")
class ImpuestoEstanciaDomainServiceTest {

    private static final ImpuestoEstanciaDomainService SERVICIO = new ImpuestoEstanciaDomainService();
    private static final ImpuestoAlojamiento COBRA = new ImpuestoAlojamiento(true, new Porcentaje(19));
    private static final Documento CC = new Documento(TipoDocumento.CC, "1094");
    private static final Documento PASAPORTE = new Documento(TipoDocumento.PASAPORTE, "AB123456");
    private static final Documento CE = new Documento(TipoDocumento.CE, "445566");
    private static final LocalDate HOY = LocalDate.of(2026, 12, 11);

    /** Dos noches de 100.000: la estancia vale 200.000 sin IVA. Ana es la huésped principal, sin documento aún. */
    private static Reserva reserva() {
        Reserva reserva = confirmada("RES-2026-00042", 11, 13);
        reserva.designarHuespedPrincipal(new OcupanteId("OCU-1"));
        return reserva;
    }

    private static void llegaCon(Reserva reserva, Documento documento) {
        reserva.completarDatosOcupante(new OcupanteId("OCU-1"), "Ana", "Ríos", null, documento, null,
                DatosViaje.VACIOS);
    }

    private static Folio folio() {
        return Folio.abrir(new FolioId("FOL-1"), new ReservaId("RES-2026-00042"), Dinero.de(200_000), HOY);
    }

    private static List<Cargo> ivasVigentes(Folio folio) {
        return folio.cargos().stream()
                .filter(c -> c.tipo() == TipoCargo.IMPUESTO
                        && folio.cargos().stream().noneMatch(o -> c.id().equals(o.corrigeA())))
                .toList();
    }

    @Test
    void alCrearConCedulaElTotalLlevaEl19PorCientoYElFolioSuCargo() {
        Reserva reserva = reserva();
        Folio folio = folio();

        SERVICIO.congelarAlCrear(reserva, folio, COBRA, CC, HOY);

        assertEquals(Dinero.de(38_000), reserva.iva());
        assertEquals(Dinero.de(238_000), reserva.valorEstancia());
        assertEquals("IVA del alojamiento (19 %)", ivasVigentes(folio).getFirst().concepto());
        assertEquals(Dinero.de(238_000), folio.saldo().monto());
    }

    @Test
    void elAnticipoSeCalculaSobreElTotalConIva() {
        Reserva reserva = reserva();
        SERVICIO.congelarAlCrear(reserva, folio(), COBRA, CC, HOY);

        // El 30 % de 238.000, no de 200.000
        assertEquals(Dinero.de(71_400), reserva.anticipoExigido(new Porcentaje(30)));
    }

    @Test
    void conCedulaDeExtranjeriaTambienSePaga() {
        Reserva reserva = reserva();
        SERVICIO.congelarAlCrear(reserva, folio(), COBRA, CE, HOY);

        assertEquals(Dinero.de(38_000), reserva.iva());
    }

    @Test
    void elTitularConPasaporteCreaLaReservaExentaYSinCargo() {
        Reserva reserva = reserva();
        Folio folio = folio();

        SERVICIO.congelarAlCrear(reserva, folio, COBRA, PASAPORTE, HOY);

        assertTrue(reserva.ivaReserva().exento());
        assertEquals(Dinero.CERO, reserva.iva());
        assertTrue(ivasVigentes(folio).isEmpty());
    }

    @Test
    void sinCobrarloNiLaReservaNiElFolioLlevanIva() {
        Reserva reserva = reserva();
        Folio folio = folio();

        SERVICIO.congelarAlCrear(reserva, folio, ImpuestoAlojamiento.INICIAL, CC, HOY);

        assertEquals(IvaReserva.NINGUNO, reserva.ivaReserva());
        assertEquals(Dinero.de(200_000), reserva.valorEstancia());
        assertTrue(ivasVigentes(folio).isEmpty());
    }

    @Test
    void siLlegaConPasaporteSeCorrigeElIvaEnElFolio() {
        Reserva reserva = reserva();
        Folio folio = folio();
        SERVICIO.congelarAlCrear(reserva, folio, COBRA, CC, HOY);
        llegaCon(reserva, PASAPORTE);

        SERVICIO.revisarEnLaLlegada(reserva, folio, CC, HOY);

        assertEquals(Dinero.CERO, reserva.iva());
        assertTrue(ivasVigentes(folio).isEmpty());
        assertTrue(folio.cargos().getLast().concepto().endsWith("llega con pasaporte"));
        assertEquals(Dinero.de(200_000), folio.saldo().monto());
    }

    @Test
    void siSeCreoConPasaporteYLlegaConCedulaSeCargaEnLaLlegada() {
        Reserva reserva = reserva();
        Folio folio = folio();
        SERVICIO.congelarAlCrear(reserva, folio, COBRA, PASAPORTE, HOY);
        llegaCon(reserva, CC);

        SERVICIO.revisarEnLaLlegada(reserva, folio, PASAPORTE, HOY);

        assertEquals(Dinero.de(38_000), reserva.iva());
        assertEquals(1, ivasVigentes(folio).size());
    }

    @Test
    void siNadaCambiaLaLlegadaNoRegistraNada() {
        Reserva reserva = reserva();
        Folio folio = folio();
        SERVICIO.congelarAlCrear(reserva, folio, COBRA, CC, HOY);
        int cargos = folio.cargos().size();
        llegaCon(reserva, CC);

        SERVICIO.revisarEnLaLlegada(reserva, folio, CC, HOY);

        assertEquals(cargos, folio.cargos().size());
    }

    @Test
    void unaReservaCreadaSinIvaSigueSinIvaEnLaLlegada() {
        // «Siguen como están»: la reserva es de antes de encender el IVA
        Reserva reserva = reserva();
        Folio folio = folio();
        llegaCon(reserva, CC);

        SERVICIO.revisarEnLaLlegada(reserva, folio, CC, HOY);

        assertEquals(Dinero.CERO, reserva.iva());
        assertTrue(ivasVigentes(folio).isEmpty());
    }

    @Test
    void laLlegadaConservaElPorcentajeCongeladoAunqueElAlojamientoLoCambie() {
        Reserva reserva = reserva();
        Folio folio = folio();
        SERVICIO.congelarAlCrear(reserva, folio, COBRA, PASAPORTE, HOY);
        llegaCon(reserva, CC);

        SERVICIO.revisarEnLaLlegada(reserva, folio, CC, HOY);

        assertEquals(19, reserva.ivaReserva().porcentaje().valor());
    }

    @Test
    void alModificarSeRecalculaConLaConfiguracionVigente() {
        Reserva reserva = reserva();
        Folio folio = folio();
        SERVICIO.congelarAlCrear(reserva, folio, COBRA, CC, HOY);

        SERVICIO.recalcularAlModificar(reserva, folio, new ImpuestoAlojamiento(true, new Porcentaje(5)), CC, HOY);

        assertEquals(Dinero.de(10_000), reserva.iva());
        assertEquals("IVA del alojamiento (5 %)", ivasVigentes(folio).getFirst().concepto());
        assertEquals(Dinero.de(210_000), folio.saldo().monto());
    }

    @Test
    void anularElIvaLoDejaEnCeroConUnaCorreccionDeSuCargo() {
        Folio folio = folio();
        folio.cargarIva(Dinero.de(38_000), new Porcentaje(19), HOY);

        folio.anularIva("cancelación", HOY);
        folio.anularIva("cancelación", HOY);

        assertTrue(ivasVigentes(folio).isEmpty());
        assertEquals("AJUSTE", folio.cargos().getLast().tipo().name());
        assertEquals(Dinero.de(200_000), folio.saldo().monto());
    }

    @Test
    void elFolioNoAceptaDosIvaVigentesPeroSiUnoDespuesDeCorregirlo() {
        Folio folio = folio();
        var primero = folio.cargarIva(Dinero.de(38_000), new Porcentaje(19), HOY);

        assertThrows(ReglaDominioException.class, () -> folio.cargarIva(Dinero.de(38_000), new Porcentaje(19), HOY));
        folio.revertirCargo(primero, "Era extranjero", HOY, new UsuarioId("USR-1"));
        assertFalse(folio.tieneIvaVigente());
    }

    @Test
    void cobrarConCeroPorCientoNoTieneSentido() {
        assertThrows(ReglaDominioException.class, () -> new ImpuestoAlojamiento(true, new Porcentaje(0)));
    }
}
