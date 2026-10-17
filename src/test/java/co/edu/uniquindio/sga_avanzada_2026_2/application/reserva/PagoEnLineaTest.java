package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PagoRechazadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PasarelaPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ModoPagoEnLinea;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PagoEnLineaDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ENTRADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reserva desde el portal, pago en línea y vencimiento por canal (B3c · DEC-69), con repositorios en memoria sobre el
 * APT-101 de {@link EscenarioEstancia}: Ana y Luis, tres noches a 100.000 por persona = 600.000; el anticipo del 30 %
 * es 180.000. Las reservas se crean el 1 de octubre a las 10:00.
 */
class PagoEnLineaTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private EscenarioEstancia escenario;
    private final List<Dinero> cobrados = new ArrayList<>();
    private boolean aprueba = true;
    private final PasarelaPago pasarela = (codigo, monto, token) -> {
        cobrados.add(monto);
        return aprueba ? new PasarelaPago.CobroEnLinea(true, "REF-1", null)
                : new PasarelaPago.CobroEnLinea(false, null, "fondos insuficientes");
    };

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(ALO).orElseThrow();
        alojamiento.habilitarMedioPago(PagoEnLineaDomainService.MEDIO);
        escenario.alojamientos.guardar(alojamiento);
    }

    private CrearReserva crearReserva(Clock reloj) {
        return new CrearReserva(escenario.cargador, escenario.reservas, escenario.folios, escenario.titulares,
                escenario.tarifas, escenario.politicas, new DisponibilidadDomainService(),
                new TarificacionDomainService(), escenario.codigos, escenario.avisos, reloj);
    }

    private ReservaPortalResult reservarDesdePortal() {
        return new ReservarDesdePortal(crearReserva(AL_CREAR), escenario.cargador, new PagoEnLineaDomainService(),
                AL_CREAR).ejecutar(new ReservarDesdePortalCommand("APT-101", ENTRADA, ENTRADA.plusDays(3),
                new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", "300 123 4567"),
                List.of(new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", "Colombiana"),
                        new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null)),
                LocalTime.of(16, 0)));
    }

    private PagarEnLinea pagarEnLinea(Clock reloj) {
        return new PagarEnLinea(escenario.cargador, escenario.reservas, escenario.folios,
                new PagoEnLineaDomainService(), pasarela, escenario.avisos, reloj);
    }

    private Folio folio(String codigo) {
        return escenario.folios.buscarPorReserva(new ReservaId(codigo)).orElseThrow();
    }

    private static Clock minutosDespues(int minutos) {
        return EscenarioEstancia.relojEn(LocalDate.of(2026, 10, 1).atTime(10, 0).plusMinutes(minutos));
    }

    private void cambiarParametros(ModoPagoEnLinea modo) {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(ALO).orElseThrow();
        ParametrosAlojamiento p = alojamiento.parametros();
        alojamiento.cambiarParametros(new ParametrosAlojamiento(p.umbralEdadFacturable(), p.horaEntrada(),
                p.horaSalida(), p.tiempoPreparacion(), p.plazoConfirmacion(), p.horaLimiteNoShow(), p.anticipo(),
                p.minimoMediosPago(), p.minimoServiciosAdicionales(), p.minimoTemporadas(),
                p.minimoTramosCancelacion(), p.minimoCapacidadesDistintas(), modo, p.plazoPagoEnLinea()));
        escenario.alojamientos.guardar(alojamiento);
    }

    @Test
    @Tag("CU-10")
    @Tag("DEC-69")
    void laReservaDelPortalQuedaPendienteConElAnticipoYMediaHoraParaPagarlo() {
        ReservaPortalResult portal = reservarDesdePortal();

        assertEquals("PENDIENTE", portal.reserva().estado());
        assertEquals("PORTAL", portal.reserva().canalOrigen());
        assertEquals(0, new BigDecimal("180000").compareTo(portal.montoAPagar()));
        assertEquals(LocalDate.of(2026, 10, 1).atTime(10, 30), portal.pagarAntesDe());
    }

    @Test
    @Tag("DEC-69")
    @Tag("RES-15")
    @Tag("MPAG-02")
    void pagarEnLineaRegistraElPagoYConfirma() {
        String codigo = reservarDesdePortal().reserva().codigo();

        ReservaResult pagada = pagarEnLinea(minutosDespues(10)).ejecutar(new PagarEnLineaCommand(codigo, "tok"));

        assertEquals("CONFIRMADA", pagada.estado());
        assertEquals(List.of(Dinero.de(180_000)), cobrados);
        assertEquals(new MedioPago("PAGO_EN_LINEA"), folio(codigo).pagos().getFirst().medio());
        assertEquals(Dinero.de(180_000), folio(codigo).totalPagado());
        assertTrue(escenario.notificaciones.enviados.getLast().asunto().startsWith("Pago recibido"));
        assertThrows(ReglaDominioException.class,
                () -> pagarEnLinea(minutosDespues(11)).ejecutar(new PagarEnLineaCommand(codigo, "tok")));
        assertEquals(1, cobrados.size());
    }

    @Test
    @Tag("DEC-69")
    void siElAlojamientoCobraElTotalSeCobraToda() {
        cambiarParametros(ModoPagoEnLinea.TOTAL);
        ReservaPortalResult portal = reservarDesdePortal();

        pagarEnLinea(minutosDespues(5)).ejecutar(new PagarEnLineaCommand(portal.reserva().codigo(), "tok"));

        assertEquals(0, new BigDecimal("600000").compareTo(portal.montoAPagar()));
        assertEquals(List.of(Dinero.de(600_000)), cobrados);
    }

    @Test
    @Tag("DEC-69")
    void unPagoRechazadoNoRegistraNadaYLaReservaSigueEsperando() {
        String codigo = reservarDesdePortal().reserva().codigo();
        aprueba = false;

        assertThrows(PagoRechazadoException.class,
                () -> pagarEnLinea(minutosDespues(5)).ejecutar(new PagarEnLineaCommand(codigo, "tok")));

        assertEquals(EstadoReserva.PENDIENTE, escenario.cargador.reserva(codigo).estado());
        assertTrue(folio(codigo).pagos().isEmpty());
    }

    @Test
    @Tag("RN-21")
    @Tag("DEC-69")
    void pasadoElPlazoNoSeCobraYLaReservaVence() {
        String delPortal = reservarDesdePortal().reserva().codigo();
        String deRecepcion = crearReserva(AL_CREAR).ejecutar(new CrearReservaCommand("APT-101", ENTRADA.plusDays(5),
                ENTRADA.plusDays(6), "DIRECTO", new TitularCommand("Eva Mora", "CC", "2077", "eva@correo.co",
                "300 555 1234"), List.of(new OcupanteCommand("Eva Mora", LocalDate.of(1985, 7, 9), "CC", "2077", null)),
                null)).codigo();
        Clock media = minutosDespues(31);

        assertThrows(ReglaDominioException.class,
                () -> pagarEnLinea(media).ejecutar(new PagarEnLineaCommand(delPortal, "tok")));
        assertTrue(cobrados.isEmpty());

        List<String> vencidas = new BuscarReservasVencidas(escenario.cargador, escenario.reservas, media)
                .ejecutar("ALO-1");
        assertEquals(List.of(delPortal), vencidas);
        new VencerReserva(escenario.cargador, escenario.reservas, escenario.folios, new CancelacionDomainService(),
                media).ejecutar(delPortal);
        assertEquals(EstadoReserva.CANCELADA, escenario.cargador.reserva(delPortal).estado());
        assertEquals(EstadoReserva.PENDIENTE, escenario.cargador.reserva(deRecepcion).estado());
    }

    @Test
    @Tag("MPAG-02")
    void sinElMedioDePagoEnLineaNoSeReservaPorElPortal() {
        Alojamiento alojamiento = escenario.alojamientos.buscarPorId(ALO).orElseThrow();
        alojamiento.deshabilitarMedioPago(PagoEnLineaDomainService.MEDIO);
        escenario.alojamientos.guardar(alojamiento);

        assertThrows(ReglaDominioException.class, this::reservarDesdePortal);
    }

    @Test
    @Tag("DEC-69")
    void elPlazoDependeDelCanal() {
        ParametrosAlojamiento p = escenario.alojamientos.buscarPorId(ALO).orElseThrow().parametros();

        assertEquals(Duration.ofMinutes(30), p.plazoParaConfirmar(CanalOrigen.PORTAL));
        assertEquals(Duration.ofHours(24), p.plazoParaConfirmar(CanalOrigen.DIRECTO));
        assertEquals(Duration.ofMinutes(30), p.plazoMasCorto());
        assertEquals(Dinero.de(120_000), ModoPagoEnLinea.ANTICIPO.exigido(Dinero.de(400_000), p.anticipo()));
        assertEquals(Dinero.de(400_000), ModoPagoEnLinea.TOTAL.exigido(Dinero.de(400_000), p.anticipo()));
    }
}
