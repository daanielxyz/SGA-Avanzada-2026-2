package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SincronizacionCanalDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ADMIN;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ENTRADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Canales, reservas externas, conflictos y bitácora sobre el APT-101 de {@link EscenarioEstancia} (capacidad 4,
 * 100.000 por ocupante y noche). El canal CAN-1 es externo; hoy es el 1 de octubre de 2026.
 */
class CanalesExternosTest {

    private static final LocalDate HOY = LocalDate.now(AL_CREAR);

    private EscenarioEstancia escenario;
    private String canal;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
        canal = registrarCanal().ejecutar(new RegistrarCanalCommand("ALO-1", "Booking", "EXTERNO", "BOOKING_API_KEY"))
                .id();
    }

    private RegistrarCanal registrarCanal() {
        return new RegistrarCanal(escenario.canales, escenario.alojamientos, escenario.codigos);
    }

    private RecibirReservaExterna recibir() {
        return new RecibirReservaExterna(escenario.cargador, escenario.canales, escenario.reservas, escenario.folios,
                escenario.titulares, escenario.tarifas, escenario.politicas, escenario.conflictos, escenario.eventos,
                new DisponibilidadDomainService(), new TarificacionDomainService(),
                new SincronizacionCanalDomainService(), escenario.codigos, AL_CREAR);
    }

    /** Ana y Luis, del día {@code desde} al {@code hasta} de la entrada del escenario. */
    private RecibirReservaExternaCommand pedido(String idExterno, int desde, int hasta) {
        return pedido(idExterno, desde, hasta,
                List.of(new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", "Colombiana"),
                        new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null)));
    }

    private RecibirReservaExternaCommand pedido(String idExterno, int desde, int hasta,
                                                List<OcupanteCommand> ocupantes) {
        return new RecibirReservaExternaCommand(canal, idExterno, "APT-101", ENTRADA.plusDays(desde),
                ENTRADA.plusDays(hasta), new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null),
                ocupantes, "{\"id\":\"" + idExterno + "\"}");
    }

    @Test
    @Tag("CAN-01")
    @Tag("CAN-02")
    void deberiaRegistrarCanalesYSoloElExternoLlevaCredencial() {
        CanalResult portal = registrarCanal().ejecutar(new RegistrarCanalCommand("ALO-1", "Portal web", "PORTAL",
                null));

        assertEquals("CAN-1", canal);
        assertEquals("CAN-2", portal.id());
        assertTrue(portal.activo());
        assertNull(portal.referenciaCredencial());
        assertEquals(List.of("Booking", "Portal web"),
                new ListarCanales(escenario.canales).ejecutar("ALO-1").stream().map(CanalResult::nombre).toList());
        assertThrows(ReglaDominioException.class, () -> registrarCanal()
                .ejecutar(new RegistrarCanalCommand("ALO-1", "Expedia", "EXTERNO", null)));
        assertThrows(RecursoNoEncontradoException.class, () -> registrarCanal()
                .ejecutar(new RegistrarCanalCommand("ALO-9", "Expedia", "EXTERNO", "EXPEDIA_KEY")));
    }

    @Test
    @Tag("CU-12")
    @Tag("CORI-03")
    @Tag("FOL-01")
    void deberiaCrearLaReservaExternaPendienteConSuFolioYAnotarlaEnLaBitacora() {
        IntegracionExternaResult resultado = recibir().ejecutar(pedido("BK-1", 0, 3));

        Reserva reserva = escenario.reserva(resultado.codigoReserva());
        assertEquals("EXITOSO", resultado.resultado());
        assertNull(resultado.conflictoId());
        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());
        assertEquals(CanalOrigen.EXTERNO, reserva.canalOrigen());
        assertEquals(new CanalId(canal), reserva.canalId());
        assertEquals("BK-1", reserva.idExterno());
        assertEquals(Dinero.de(600_000), escenario.folio(resultado.codigoReserva()).saldo().monto());
        assertEquals(1, escenario.titulares.guardados.size());
        EventoCanal evento = escenario.eventos.registrados.getFirst();
        assertEquals(ResultadoEvento.EXITOSO, evento.resultado());
        assertEquals(resultado.codigoReserva(), evento.detalle());
        assertEquals("{\"id\":\"BK-1\"}", evento.cargaUtil());
    }

    @Test
    @Tag("RN-19")
    @Tag("CAN-04")
    void elMismoIdentificadorDelCanalNoCreaOtraReserva() {
        IntegracionExternaResult primera = recibir().ejecutar(pedido("BK-1", 0, 3));

        IntegracionExternaResult repetida = recibir().ejecutar(pedido("BK-1", 0, 3));

        assertEquals("DUPLICADO", repetida.resultado());
        assertEquals(primera.codigoReserva(), repetida.codigoReserva());
        assertEquals(1, escenario.reservas.guardadas.size());
        assertEquals(1, escenario.folios.guardados.size());
        assertTrue(escenario.conflictos.guardados.isEmpty());
        assertEquals(List.of(ResultadoEvento.EXITOSO, ResultadoEvento.DUPLICADO),
                escenario.eventos.registrados.stream().map(EventoCanal::resultado).toList());
    }

    @Test
    @Tag("RN-18")
    @Tag("RN-01")
    @Tag("CONF-01")
    @Tag("CONF-02")
    void unaReservaExternaSinDisponibilidadRegistraUnConflictoYNoTocaLaVigente() {
        String vigente = escenario.reservaConfirmada(); // ocupa del 10 al 13 de octubre

        IntegracionExternaResult resultado = recibir().ejecutar(pedido("BK-2", 2, 5));

        assertEquals("CONFLICTO", resultado.resultado());
        assertNull(resultado.codigoReserva());
        assertEquals(1, escenario.reservas.guardadas.size());
        assertEquals(EstadoReserva.CONFIRMADA, escenario.reserva(vigente).estado());
        ConflictoResult conflicto = new ConsultarConflictosPendientes(escenario.conflictos).ejecutar().getFirst();
        assertEquals(resultado.conflictoId(), conflicto.id());
        assertEquals("BK-2", conflicto.idExterno());
        assertEquals("APT-101", conflicto.apartamento());
        assertEquals(ENTRADA.plusDays(2), conflicto.entrada());
        assertEquals(resultado.detalle(), conflicto.motivo());
        assertEquals(ResultadoEvento.CONFLICTO, escenario.eventos.registrados.getFirst().resultado());
    }

    @Test
    @Tag("RN-02")
    @Tag("DEC-48")
    void unGrupoQueNoCabeTambienQuedaComoConflicto() {
        List<OcupanteCommand> cinco = List.of(
                new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", null),
                new OcupanteCommand("Luis", LocalDate.of(1988, 3, 2), null, null, null),
                new OcupanteCommand("Eva", LocalDate.of(2000, 1, 1), null, null, null),
                new OcupanteCommand("Leo", LocalDate.of(2001, 1, 1), null, null, null),
                new OcupanteCommand("Sol", LocalDate.of(2002, 1, 1), null, null, null));

        IntegracionExternaResult resultado = recibir().ejecutar(pedido("BK-3", 0, 2, cinco));

        assertEquals("CONFLICTO", resultado.resultado());
        assertTrue(escenario.reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("TIT-01")
    @Tag("DEC-62")
    void unRechazoPorOtraReglaNoGuardaNadaNiRegistraConflicto() {
        List<OcupanteCommand> sinTitular = List.of(
                new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null));

        assertThrows(ReglaDominioException.class, () -> recibir().ejecutar(pedido("BK-4", 0, 2, sinTitular)));

        assertTrue(escenario.reservas.guardadas.isEmpty());
        assertTrue(escenario.folios.guardados.isEmpty());
        assertTrue(escenario.titulares.guardados.isEmpty());
        assertTrue(escenario.conflictos.guardados.isEmpty());
        assertTrue(escenario.eventos.registrados.isEmpty());
    }

    @Test
    @Tag("CAN-05")
    void unCanalDesactivadoONoExternoNoTraeReservas() {
        String portal = registrarCanal().ejecutar(new RegistrarCanalCommand("ALO-1", "Portal", "PORTAL", null)).id();
        RecibirReservaExternaCommand porElPortal = new RecibirReservaExternaCommand(portal, "X-1", "APT-101",
                ENTRADA, ENTRADA.plusDays(2), new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null),
                pedido("X-1", 0, 2).ocupantes(), "{}");
        DesactivarCanal desactivar = new DesactivarCanal(escenario.canales);

        assertThrows(ReglaDominioException.class, () -> recibir().ejecutar(porElPortal));
        assertFalse(desactivar.ejecutar(canal).activo());
        assertThrows(ReglaDominioException.class, () -> recibir().ejecutar(pedido("BK-5", 0, 2)));
        assertThrows(ReglaDominioException.class, () -> desactivar.ejecutar(canal));
        assertThrows(RecursoNoEncontradoException.class, () -> desactivar.ejecutar("CAN-99"));
        assertTrue(escenario.reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("DEC-26")
    void unCanalNoVendeApartamentosDeOtroAlojamiento() {
        escenario.alojamientos.guardar(
                RepositoriosEnMemoria.puertaAlSol("ALO-2"));
        canal = registrarCanal().ejecutar(new RegistrarCanalCommand("ALO-2", "Booking 2", "EXTERNO", "KEY_2")).id();

        assertThrows(RecursoNoEncontradoException.class, () -> recibir().ejecutar(pedido("BK-6", 0, 2)));
    }

    @Test
    @Tag("CONF-04")
    void elAdministradorResuelveUnConflictoDejandoSuDecision() {
        escenario.reservaConfirmada();
        String conflictoId = recibir().ejecutar(pedido("BK-2", 2, 5)).conflictoId();
        ResolverConflicto resolver = new ResolverConflicto(escenario.conflictos, AL_CREAR);

        assertThrows(ReglaDominioException.class,
                () -> resolver.ejecutar(new ResolverConflictoCommand(conflictoId, " ", ADMIN)));
        ConflictoResult resuelto = resolver.ejecutar(
                new ResolverConflictoCommand(conflictoId, "Se reubicó en el APT-102", ADMIN));

        assertEquals("RESUELTO", resuelto.estado());
        assertEquals(ADMIN, resuelto.resueltoPor());
        assertEquals("Se reubicó en el APT-102", resuelto.decision());
        assertTrue(new ConsultarConflictosPendientes(escenario.conflictos).ejecutar().isEmpty());
        assertThrows(ReglaDominioException.class,
                () -> resolver.ejecutar(new ResolverConflictoCommand(conflictoId, "Otra", ADMIN)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> resolver.ejecutar(new ResolverConflictoCommand("CON-99", "Otra", ADMIN)));
    }

    @Test
    @Tag("BIT-01")
    @Tag("BIT-03")
    void laBitacoraGuardaTambienLosRechazosYSeConsultaPorDias() {
        recibir().ejecutar(pedido("BK-1", 0, 3));
        RegistrarEventoCanal registrar = new RegistrarEventoCanal(escenario.eventos, escenario.canales,
                escenario.codigos, AL_CREAR);

        EventoCanalResult rechazo = registrar.ejecutar(new RegistrarEventoCanalCommand(canal, "CREAR_RESERVA",
                "ENTRANTE", "{\"id\":\"BK-4\"}", "RECHAZADO", "El titular debe ser uno de los ocupantes"));

        ConsultarBitacora consultar = new ConsultarBitacora(escenario.eventos, escenario.canales);
        Pagina<EventoCanalResult> deHoy = consultar.ejecutar(new ConsultarBitacoraCommand(canal, HOY, HOY, 0));
        assertEquals("RECHAZADO", rechazo.resultado());
        assertEquals(2, deHoy.totalElementos());
        assertEquals(List.of("EXITOSO", "RECHAZADO"),
                deHoy.contenido().stream().map(EventoCanalResult::resultado).sorted().toList());
        assertTrue(consultar.ejecutar(new ConsultarBitacoraCommand(canal, HOY.plusDays(1), HOY.plusDays(2), 0))
                .contenido().isEmpty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> consultar.ejecutar(new ConsultarBitacoraCommand("CAN-99", HOY, HOY, 0)));
    }
}
