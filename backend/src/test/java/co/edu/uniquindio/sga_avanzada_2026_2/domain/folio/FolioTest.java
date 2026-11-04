package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);
    private static final MedioPago EFECTIVO = new MedioPago("EFECTIVO");
    private static final MedioPago TARJETA = new MedioPago("TARJETA");
    private static final Set<MedioPago> HABILITADOS = Set.of(EFECTIVO, TARJETA);
    private static final AutorizacionCierre AUTORIZACION = new AutorizacionCierre(new UsuarioId("ADM-1"),
            "Cortesía por inconvenientes", HOY.atTime(12, 0));

    private static Folio folio() {
        return Folio.abrir(new FolioId("FOL-1"), new ReservaId("RES-2026-00001"), Dinero.de(400_000), HOY);
    }

    private static Saldo saldo(long monto, SituacionSaldo situacion) {
        return new Saldo(Dinero.de(monto), situacion);
    }

    @Test
    @Tag("FOL-01")
    @Tag("CAR-04")
    void deberiaAbrirseConElCargoDeAlojamiento() {
        Folio folio = folio();

        Cargo alojamiento = folio.cargos().getFirst();
        assertEquals(TipoCargo.HOSPEDAJE, alojamiento.tipo());
        assertEquals(new CargoId("CAR-1"), alojamiento.id());
        assertEquals(saldo(400_000, SituacionSaldo.PENDIENTE), folio.saldo());
        assertFalse(folio.cerrado());
    }

    @Test
    @Tag("RN-15")
    @Tag("PAG-05")
    void deberiaRegistrarPagoYRecalcularElSaldo() {
        Folio folio = folio();

        folio.registrarPago(EFECTIVO, Dinero.de(120_000), HOY, HOY, HABILITADOS);
        PagoId segundo = folio.registrarPago(TARJETA, Dinero.de(80_000), HOY.minusDays(1), HOY, HABILITADOS);

        assertEquals(new PagoId("PAG-2"), segundo);
        assertEquals(Dinero.de(200_000), folio.totalPagado());
        assertEquals(saldo(200_000, SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("RN-15")
    void deberiaRechazarPagoSinMedioOSinFecha() {
        Folio folio = folio();

        assertThrows(ReglaDominioException.class,
                () -> folio.registrarPago(null, Dinero.de(1_000), HOY, HOY, HABILITADOS));
        assertThrows(ReglaDominioException.class,
                () -> folio.registrarPago(EFECTIVO, Dinero.de(1_000), null, HOY, HABILITADOS));
        assertThrows(ReglaDominioException.class,
                () -> folio.registrarPago(EFECTIVO, Dinero.CERO, HOY, HOY, HABILITADOS));
        assertTrue(folio.pagos().isEmpty());
    }

    @Test
    @Tag("MPAG-02")
    void deberiaRechazarMedioDePagoDeshabilitado() {
        assertThrows(ReglaDominioException.class, () -> folio().registrarPago(new MedioPago("CRIPTO"),
                Dinero.de(1_000), HOY, HOY, HABILITADOS));
    }

    @Test
    @Tag("PAG-07")
    void deberiaRechazarPagoConFechaFutura() {
        assertThrows(ReglaDominioException.class,
                () -> folio().registrarPago(EFECTIVO, Dinero.de(1_000), HOY.plusDays(1), HOY, HABILITADOS));
    }

    @Test
    @Tag("RN-16")
    @Tag("CAR-03")
    void crearMovimientoInversoAlAjustarCargoFolio() {
        Folio folio = folio();
        CargoId servicio = folio.agregarServicioAdicional("Desayuno", Dinero.de(25_000), HOY);

        CargoId correccion = folio.revertirCargo(servicio, "No lo tomó", HOY);

        Cargo ajuste = folio.cargos().getLast();
        assertEquals(correccion, ajuste.id());
        assertEquals(TipoCargo.AJUSTE, ajuste.tipo());
        assertEquals(SentidoAjuste.DISMINUYE, ajuste.sentido());
        assertEquals(servicio, ajuste.corrigeA());
        assertEquals(3, folio.cargos().size()); // el original sigue ahí
        assertEquals(saldo(400_000, SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("RN-16")
    void deberiaRechazarEditarOBorrarUnMovimientoExistente() {
        Folio folio = folio();
        CargoId alojamiento = folio.cargos().getFirst().id();
        folio.revertirCargo(alojamiento, "Error", HOY);

        assertThrows(UnsupportedOperationException.class, () -> folio.cargos().clear());
        assertThrows(ReglaDominioException.class, () -> folio.revertirCargo(alojamiento, "Otra vez", HOY));
        assertThrows(ReglaDominioException.class, () -> folio.revertirCargo(new CargoId("CAR-2"), "x", HOY));
    }

    @Test
    @Tag("PAG-03")
    @Tag("TPAG-02")
    void deberiaRevertirUnAbonoUnaSolaVez() {
        Folio folio = folio();
        PagoId abono = folio.registrarPago(TARJETA, Dinero.de(100_000), HOY, HOY, HABILITADOS);

        PagoId reverso = folio.revertirPago(abono, HOY, HOY);

        Pago registrado = folio.pagos().getLast();
        assertEquals(reverso, registrado.id());
        assertTrue(registrado.esReverso());
        assertEquals(abono, registrado.reversaDe());
        assertEquals(TARJETA, registrado.medio());
        assertEquals(Dinero.CERO, folio.totalPagado());
        assertThrows(ReglaDominioException.class, () -> folio.revertirPago(abono, HOY, HOY));
        assertThrows(ReglaDominioException.class, () -> folio.revertirPago(reverso, HOY, HOY));
    }

    @Test
    @Tag("PAG-06")
    @Tag("TPAG-02")
    void unaDevolucionNoPuedeSuperarLoAbonado() {
        Folio folio = folio();
        folio.registrarPago(EFECTIVO, Dinero.de(100_000), HOY, HOY, HABILITADOS);

        assertThrows(ReglaDominioException.class,
                () -> folio.registrarDevolucion(EFECTIVO, Dinero.de(100_001), HOY, HOY, HABILITADOS));
        folio.registrarDevolucion(EFECTIVO, Dinero.de(40_000), HOY, HOY, HABILITADOS);

        assertEquals(Dinero.de(60_000), folio.totalPagado());
        assertEquals(null, folio.pagos().getLast().reversaDe());
    }

    @Test
    @Tag("RN-14")
    @Tag("OCU-06")
    void deberiaRegistrarLaDiferenciaDeUnaModificacionComoAjuste() {
        Folio folio = folio();

        folio.ajustarPorModificacion(Dinero.de(400_000), Dinero.de(500_000), HOY);
        assertEquals(SentidoAjuste.AUMENTA, folio.cargos().getLast().sentido());
        folio.ajustarPorModificacion(Dinero.de(500_000), Dinero.de(300_000), HOY);
        assertEquals(SentidoAjuste.DISMINUYE, folio.cargos().getLast().sentido());
        folio.ajustarPorModificacion(Dinero.de(300_000), Dinero.de(300_000), HOY);

        assertEquals(3, folio.cargos().size());
        assertEquals(saldo(300_000, SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    @Tag("SLD-03")
    @Tag("CAR-02")
    void deberiaQuedarSaldoAFavorSiLoPagadoSuperaLaPenalidad() {
        Folio folio = folio();
        folio.registrarPago(EFECTIVO, Dinero.de(120_000), HOY, HOY, HABILITADOS);

        folio.liquidarPenalidad(Dinero.de(400_000), Dinero.de(60_000), "cancelación", HOY);

        assertEquals(TipoCargo.PENALIDAD_CANCELACION, folio.cargos().getLast().tipo());
        assertEquals(saldo(60_000, SituacionSaldo.A_FAVOR), folio.saldo());
    }

    @Test
    @Tag("CAR-06")
    void sinPenalidadSoloSeAnulaElAlojamiento() {
        Folio folio = folio();

        folio.liquidarPenalidad(Dinero.de(400_000), Dinero.CERO, "cancelación", HOY);

        assertEquals(2, folio.cargos().size());
        assertEquals(saldo(0, SituacionSaldo.AL_DIA), folio.saldo());
    }

    @Test
    @Tag("RN-17")
    void deberiaCerrarFolioConSaldoCero() {
        Folio folio = folio();
        folio.registrarPago(EFECTIVO, Dinero.de(400_000), HOY, HOY, HABILITADOS);

        folio.cerrar();

        assertTrue(folio.cerrado());
        assertEquals(null, folio.autorizacion());
    }

    @Test
    @Tag("RN-17")
    void deberiaCerrarFolioConSaldoPendienteConAutorizacion() {
        Folio folio = folio();

        folio.cerrar(AUTORIZACION);

        assertTrue(folio.cerrado());
        assertEquals(AUTORIZACION, folio.autorizacion());
    }

    @Test
    @Tag("RN-17")
    void siFolioCerrarYSaldoFinalDiferenteCeroSinAutorizacionRechazar() {
        Folio folio = folio();

        assertThrows(ReglaDominioException.class, folio::cerrar);
        assertFalse(folio.cerrado());
    }

    @Test
    @Tag("FOL-06")
    @Tag("CAR-07")
    void unFolioCerradoNoAdmiteMovimientos() {
        Folio folio = folio();
        folio.cerrar(AUTORIZACION);

        assertThrows(ReglaDominioException.class,
                () -> folio.registrarPago(EFECTIVO, Dinero.de(1_000), HOY, HOY, HABILITADOS));
        assertThrows(ReglaDominioException.class,
                () -> folio.agregarServicioAdicional("Desayuno", Dinero.de(25_000), HOY));
        assertThrows(ReglaDominioException.class,
                () -> folio.liquidarPenalidad(Dinero.de(400_000), Dinero.CERO, "cancelación", HOY));
        assertThrows(ReglaDominioException.class, () -> folio.cerrar(AUTORIZACION));
    }

    @Test
    @Tag("CAR-06")
    void soloUnAjusteLlevaSentidoYTodoCargoEsPositivo() {
        CargoId id = new CargoId("CAR-1");

        assertThrows(ReglaDominioException.class,
                () -> new Cargo(id, TipoCargo.HOSPEDAJE, "x", Dinero.de(1), SentidoAjuste.DISMINUYE, HOY, null));
        assertThrows(ReglaDominioException.class,
                () -> new Cargo(id, TipoCargo.AJUSTE, "x", Dinero.de(1), null, HOY, null));
        assertThrows(ReglaDominioException.class,
                () -> new Cargo(id, TipoCargo.SERVICIO_ADICIONAL, "x", Dinero.CERO, null, HOY, null));
    }

    @Test
    @Tag("FOL-05")
    void noDeberiaReconstruirUnaAutorizacionEnUnFolioAbierto() {
        assertThrows(ReglaDominioException.class, () -> new Folio(new FolioId("FOL-1"),
                new ReservaId("RES-2026-00001"), List.of(), List.of(), false, AUTORIZACION));
    }
}
