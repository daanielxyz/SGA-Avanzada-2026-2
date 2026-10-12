package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CargoServicioDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ADMIN;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ALOJADO;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.RECEPCION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos de uso del folio sobre la reserva de {@link EscenarioEstancia}: 600.000 de alojamiento; el catálogo del
 * alojamiento tiene el Desayuno (SRV-1) a 25.000 y acepta EFECTIVO y TARJETA.
 */
class GestionarFolioTest {

    private static final LocalDate HOY = LocalDate.now(AL_CREAR);

    private EscenarioEstancia escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
    }

    private static void assertMonto(long esperado, BigDecimal real) {
        assertEquals(0, BigDecimal.valueOf(esperado).compareTo(real), "esperado " + esperado + " y fue " + real);
    }

    private RegistrarPago registrarPago() {
        return new RegistrarPago(escenario.cargador, escenario.folios, AL_CREAR);
    }

    private static RegistrarPagoCommand pago(String codigo, String medio, long monto) {
        return new RegistrarPagoCommand(codigo, medio, BigDecimal.valueOf(monto), HOY);
    }

    private CargarServicioAdicional cargarServicio(Clock reloj) {
        return new CargarServicioAdicional(escenario.cargador, escenario.folios, new CargoServicioDomainService(),
                reloj);
    }

    private CerrarFolio cerrarFolio() {
        return new CerrarFolio(escenario.cargador, escenario.folios, AL_CREAR);
    }

    @Test
    @Tag("FOL-01")
    @Tag("FOL-03")
    void deberiaConsultarElFolioDeLaReservaConSuSaldoCalculado() {
        String codigo = escenario.reservaPendiente();

        FolioResult folio = new ConsultarFolio(escenario.cargador).ejecutar(codigo);

        assertEquals("FOL-1", folio.id());
        assertEquals(codigo, folio.reserva());
        assertEquals("HOSPEDAJE", folio.cargos().getFirst().tipo());
        assertMonto(600_000, folio.saldo());
        assertEquals("PENDIENTE", folio.situacionSaldo());
        assertFalse(folio.cerrado());
        assertNull(folio.autorizacion());
    }

    @Test
    void deberiaRechazarElFolioDeUnaReservaInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> new ConsultarFolio(escenario.cargador).ejecutar("RES-2026-09999"));
    }

    @Test
    @Tag("RN-15")
    @Tag("PAG-05")
    void deberiaRegistrarPagosParcialesConMediosDistintos() {
        String codigo = escenario.reservaPendiente();

        registrarPago().ejecutar(pago(codigo, "efectivo", 200_000));
        FolioResult folio = registrarPago().ejecutar(pago(codigo, "TARJETA", 100_000));

        assertEquals(2, folio.pagos().size());
        assertEquals("EFECTIVO", folio.pagos().getFirst().medio());
        assertEquals("ABONO", folio.pagos().getLast().tipo());
        assertMonto(300_000, folio.totalPagado());
        assertMonto(300_000, folio.saldo());
    }

    @Test
    @Tag("MPAG-02")
    void noDeberiaRegistrarUnPagoConUnMedioQueElAlojamientoNoHabilito() {
        String codigo = escenario.reservaPendiente();

        assertThrows(ReglaDominioException.class, () -> registrarPago().ejecutar(pago(codigo, "CRIPTO", 100_000)));
        assertTrue(escenario.folio(codigo).pagos().isEmpty());
    }

    @Test
    @Tag("PAG-07")
    @Tag("PAG-02")
    void noDeberiaRegistrarUnPagoFuturoNiSinMonto() {
        String codigo = escenario.reservaPendiente();

        assertThrows(ReglaDominioException.class, () -> registrarPago().ejecutar(
                new RegistrarPagoCommand(codigo, "EFECTIVO", BigDecimal.valueOf(100_000), HOY.plusDays(1))));
        assertThrows(ReglaDominioException.class, () -> registrarPago().ejecutar(pago(codigo, "EFECTIVO", 0)));
    }

    @Test
    @Tag("RN-16")
    @Tag("PAG-03")
    void deberiaRevertirUnPagoMalRegistradoSinBorrarlo() {
        String codigo = escenario.reservaPendiente();
        registrarPago().ejecutar(pago(codigo, "EFECTIVO", 200_000));
        RevertirPago revertir = new RevertirPago(escenario.cargador, escenario.folios, AL_CREAR);

        FolioResult folio = revertir.ejecutar(new RevertirPagoCommand(codigo, "PAG-1"));

        assertEquals(2, folio.pagos().size());
        assertEquals("REVERSO", folio.pagos().getLast().tipo());
        assertEquals("PAG-1", folio.pagos().getLast().reversaDe());
        assertMonto(0, folio.totalPagado());
        assertThrows(ReglaDominioException.class, () -> revertir.ejecutar(new RevertirPagoCommand(codigo, "PAG-1")));
        assertThrows(ReglaDominioException.class, () -> revertir.ejecutar(new RevertirPagoCommand(codigo, "PAG-9")));
    }

    @Test
    @Tag("PAG-06")
    @Tag("TPAG-02")
    void deberiaRegistrarLaDevolucionDelSaldoAFavorTrasCancelar() {
        String codigo = escenario.reservaConfirmada(); // pagó 180.000 y cancela gratis, con más de 48 h
        new CancelarReserva(escenario.cargador, escenario.reservas, escenario.folios, escenario.politicas,
                new CancelacionDomainService(), AL_CREAR).ejecutar(codigo);
        RegistrarDevolucion devolver = new RegistrarDevolucion(escenario.cargador, escenario.folios, AL_CREAR);

        assertThrows(ReglaDominioException.class, () -> devolver.ejecutar(pago(codigo, "EFECTIVO", 200_000)));
        FolioResult folio = devolver.ejecutar(pago(codigo, "EFECTIVO", 180_000));

        assertEquals("REVERSO", folio.pagos().getLast().tipo());
        assertNull(folio.pagos().getLast().reversaDe());
        assertEquals("AL_DIA", folio.situacionSaldo());
    }

    @Test
    @Tag("CAR-05")
    void deberiaCobrarUnServicioAdicionalDuranteLaEstancia() {
        String codigo = escenario.reservaEnCurso();

        FolioResult folio = cargarServicio(ALOJADO).ejecutar(new CargarServicioAdicionalCommand(codigo, "SRV-1"));

        assertEquals("SERVICIO_ADICIONAL", folio.cargos().getLast().tipo());
        assertEquals("Desayuno", folio.cargos().getLast().concepto());
        assertEquals(LocalDate.now(ALOJADO), folio.cargos().getLast().fecha());
        assertMonto(445_000, folio.saldo()); // 600.000 + 25.000 − 180.000 del anticipo
    }

    @Test
    @Tag("DEC-60")
    void noDeberiaCobrarUnServicioAntesDeLaLlegada() {
        String codigo = escenario.reservaConfirmada();

        assertThrows(ReglaDominioException.class,
                () -> cargarServicio(AL_CREAR).ejecutar(new CargarServicioAdicionalCommand(codigo, "SRV-1")));
        assertThrows(ReglaDominioException.class,
                () -> cargarServicio(AL_CREAR).ejecutar(new CargarServicioAdicionalCommand(codigo, "SRV-9")));
        assertEquals(1, escenario.folio(codigo).cargos().size());
    }

    @Test
    @Tag("RN-16")
    @Tag("CAR-03")
    void deberiaRevertirUnCargoMalRegistradoConUnAjusteContrario() {
        String codigo = escenario.reservaEnCurso();
        cargarServicio(ALOJADO).ejecutar(new CargarServicioAdicionalCommand(codigo, "SRV-1"));
        RevertirCargo revertir = new RevertirCargo(escenario.cargador, escenario.folios, ALOJADO);

        FolioResult folio = revertir.ejecutar(new RevertirCargoCommand(codigo, "CAR-2", "Se cobró a otro apartamento"));

        assertEquals(3, folio.cargos().size());
        assertEquals("AJUSTE", folio.cargos().getLast().tipo());
        assertEquals("DISMINUYE", folio.cargos().getLast().sentido());
        assertEquals("CAR-2", folio.cargos().getLast().corrigeA());
        assertMonto(420_000, folio.saldo());
        assertThrows(ReglaDominioException.class,
                () -> revertir.ejecutar(new RevertirCargoCommand(codigo, "CAR-2", "Otra vez")));
    }

    @Test
    @Tag("RN-17")
    @Tag("FOL-05")
    void noDeberiaCerrarUnFolioConSaldoSinAutorizacion() {
        String codigo = escenario.reservaPendiente();

        assertThrows(ReglaDominioException.class,
                () -> cerrarFolio().ejecutar(new CerrarFolioCommand(codigo, RECEPCION, null)));
        assertThrows(ReglaDominioException.class,
                () -> cerrarFolio().ejecutar(new CerrarFolioCommand(codigo, RECEPCION, "  ")));
        assertFalse(escenario.folio(codigo).cerrado());
    }

    @Test
    @Tag("RN-17")
    @Tag("FOL-06")
    void deberiaCerrarElFolioPagadoYNoAdmitirMasMovimientos() {
        String codigo = escenario.reservaPendiente();
        registrarPago().ejecutar(pago(codigo, "EFECTIVO", 600_000));

        FolioResult folio = cerrarFolio().ejecutar(new CerrarFolioCommand(codigo, RECEPCION, null));

        assertTrue(folio.cerrado());
        assertNull(folio.autorizacion());
        assertThrows(ReglaDominioException.class, () -> registrarPago().ejecutar(pago(codigo, "EFECTIVO", 1_000)));
    }

    @Test
    @Tag("RN-17")
    @Tag("AUTC-04")
    void deberiaCerrarConSaldoSiHayAutorizacionRegistradaSinCambiarElSaldo() {
        String codigo = escenario.reservaPendiente();

        FolioResult folio = cerrarFolio().ejecutar(new CerrarFolioCommand(codigo, ADMIN, "Convenio con la empresa"));

        assertTrue(folio.cerrado());
        assertEquals(ADMIN, folio.autorizacion().autor());
        assertEquals("Convenio con la empresa", folio.autorizacion().motivo());
        assertMonto(600_000, folio.saldo());
    }
}
