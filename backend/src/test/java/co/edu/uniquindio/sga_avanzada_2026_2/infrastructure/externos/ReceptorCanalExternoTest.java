package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CancelarReservaExternaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarDisponibilidadCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CancelarReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarDisponibilidadCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SincronizacionCanalDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AutenticadorCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.CanalNoAutenticadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.LocalDate;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ENTRADA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El adaptador de entrada de los canales con los casos de uso reales y repositorios en memoria, sobre el APT-101 de
 * {@link EscenarioEstancia}: lo que el caso de uso rechaza también debe quedar en la bitácora.
 */
class ReceptorCanalExternoTest {

    private static final String CLAVE = "clave-booking";

    private EscenarioEstancia escenario;
    private ReceptorCanalExterno receptor;
    private String canal;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
        canal = new RegistrarCanalUseCase(escenario.canales, escenario.alojamientos, escenario.codigos)
                .ejecutar(new RegistrarCanalCommand("ALO-1", "Booking", "EXTERNO", "BOOKING_API_KEY")).id();
        MockEnvironment entorno = new MockEnvironment().withProperty("sga.canales.BOOKING_API_KEY", CLAVE);
        RegistrarEventoCanalUseCase registrarEvento = new RegistrarEventoCanalUseCase(escenario.eventos,
                escenario.canales,
                escenario.codigos, AL_CREAR);
        ConsultarDisponibilidadCanalUseCase consultar = new ConsultarDisponibilidadCanalUseCase(escenario.canales,
                escenario.eventos, new BuscarDisponiblesUseCase(escenario.cargador, escenario.apartamentos,
                escenario.reservas, new DisponibilidadDomainService()), escenario.codigos, AL_CREAR);
        CancelarReservaExternaUseCase cancelar = new CancelarReservaExternaUseCase(escenario.canales,
                escenario.reservas,
                escenario.eventos, new CancelarReservaUseCase(escenario.cargador, escenario.reservas, escenario.folios,
                escenario.politicas, new CancelacionDomainService(), AL_CREAR), escenario.codigos, AL_CREAR);
        receptor = new ReceptorCanalExterno(new AutenticadorCanal(escenario.canales, entorno),
                new RecibirReservaExternaUseCase(escenario.cargador, escenario.canales, escenario.reservas,
                        escenario.folios, escenario.titulares, escenario.tarifas, escenario.politicas,
                        escenario.conflictos, escenario.eventos, new DisponibilidadDomainService(),
                        new TarificacionDomainService(), new SincronizacionCanalDomainService(), escenario.codigos,
                        escenario.avisos, AL_CREAR),
                consultar, cancelar, registrarEvento);
    }

    private RecibirReservaExternaCommand pedido(String canalId, String idExterno, List<OcupanteCommand> ocupantes) {
        return new RecibirReservaExternaCommand(canalId, idExterno, "APT-101", ENTRADA, ENTRADA.plusDays(2),
                new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null), ocupantes,
                "{\"idExterno\":\"" + idExterno + "\"}");
    }

    private static List<OcupanteCommand> conAna() {
        return List.of(new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", null));
    }

    @Test
    @Tag("CU-12")
    void unaReservaQueSeIntegraNoSeAnotaDosVeces() {
        IntegracionExternaResult resultado = receptor.recibir(CLAVE, pedido(canal, "BK-1", conAna()));

        assertEquals("EXITOSO", resultado.resultado());
        assertEquals(List.of(ResultadoEvento.EXITOSO),
                escenario.eventos.registrados.stream().map(EventoCanal::resultado).toList());
    }

    @Test
    @Tag("BIT-03")
    @Tag("DEC-62")
    void unRechazoQuedaEnLaBitacoraYElCanalRecibeElMismoError() {
        List<OcupanteCommand> sinElTitular = List.of(
                new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null));

        ReglaDominioException rechazo = assertThrows(ReglaDominioException.class,
                () -> receptor.recibir(CLAVE, pedido(canal, "BK-2", sinElTitular)));

        EventoCanal evento = escenario.eventos.registrados.getFirst();
        assertEquals(1, escenario.eventos.registrados.size());
        assertEquals(ResultadoEvento.RECHAZADO, evento.resultado());
        assertEquals(rechazo.getMessage(), evento.detalle());
        assertEquals("{\"idExterno\":\"BK-2\"}", evento.cargaUtil());
        assertTrue(escenario.reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("BIT-03")
    void unApartamentoInexistenteTambienSeAnotaComoRechazo() {
        RecibirReservaExternaCommand aOtroApartamento = new RecibirReservaExternaCommand(canal, "BK-3", "APT-999",
                ENTRADA, ENTRADA.plusDays(2), new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null),
                conAna(), "{}");

        assertThrows(RecursoNoEncontradoException.class, () -> receptor.recibir(CLAVE, aOtroApartamento));

        assertEquals(ResultadoEvento.RECHAZADO, escenario.eventos.registrados.getFirst().resultado());
    }

    @Test
    @Tag("CAN-03")
    @Tag("DEC-73")
    void sinLaClaveDelCanalNoSeIntegraNiSeAnota() {
        assertThrows(CanalNoAutenticadoException.class,
                () -> receptor.recibir("otra", pedido(canal, "BK-4", conAna())));
        assertThrows(CanalNoAutenticadoException.class,
                () -> receptor.recibir(null, pedido(canal, "BK-4", conAna())));
        assertThrows(CanalNoAutenticadoException.class,
                () -> receptor.recibir(CLAVE, pedido("CAN-99", "BK-4", conAna())));

        assertTrue(escenario.eventos.registrados.isEmpty());
        assertTrue(escenario.reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("CU-13")
    @Tag("CU-16")
    @Tag("BIT-01")
    void consultarYCancelarQuedanEnLaBitacora() {
        IntegracionExternaResult creada = receptor.recibir(CLAVE, pedido(canal, "BK-5", conAna()));

        assertEquals(List.of("APT-101"), receptor.consultarDisponibilidad(CLAVE,
                new ConsultarDisponibilidadCanalCommand(canal, ENTRADA.plusDays(5), ENTRADA.plusDays(6), 2, "{}"))
                .stream().map(ApartamentoDisponibleResult::codigo).toList());
        assertEquals("CANCELADA", receptor.cancelar(CLAVE, new CancelarReservaExternaCommand(canal, "BK-5", "{}"))
                .estado());
        assertThrows(RecursoNoEncontradoException.class,
                () -> receptor.cancelar(CLAVE, new CancelarReservaExternaCommand(canal, "BK-NO", "{}")));

        assertEquals(EstadoReserva.CANCELADA, escenario.cargador.reserva(creada.codigoReserva()).estado());
        assertEquals(List.of(OperacionCanal.CREAR_RESERVA, OperacionCanal.CONSULTAR_DISPONIBILIDAD,
                        OperacionCanal.CANCELAR_RESERVA, OperacionCanal.CANCELAR_RESERVA),
                escenario.eventos.registrados.stream().map(EventoCanal::operacion).toList());
        assertEquals(ResultadoEvento.RECHAZADO, escenario.eventos.registrados.getLast().resultado());
    }
}
