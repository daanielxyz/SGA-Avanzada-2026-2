package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExterna;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SincronizacionCanalDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
class ReceptorReservaExternaTest {

    private EscenarioEstancia escenario;
    private ReceptorReservaExterna receptor;
    private String canal;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
        canal = new RegistrarCanal(escenario.canales, escenario.alojamientos, escenario.codigos)
                .ejecutar(new RegistrarCanalCommand("ALO-1", "Booking", "EXTERNO", "BOOKING_API_KEY")).id();
        receptor = new ReceptorReservaExterna(
                new RecibirReservaExterna(escenario.cargador, escenario.canales, escenario.reservas,
                        escenario.folios, escenario.titulares, escenario.tarifas, escenario.politicas,
                        escenario.conflictos, escenario.eventos, new DisponibilidadDomainService(),
                        new TarificacionDomainService(), new SincronizacionCanalDomainService(), escenario.codigos,
                        escenario.avisos, AL_CREAR),
                new RegistrarEventoCanal(escenario.eventos, escenario.canales, escenario.codigos, AL_CREAR));
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
        IntegracionExternaResult resultado = receptor.recibir(pedido(canal, "BK-1", conAna()));

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
                () -> receptor.recibir(pedido(canal, "BK-2", sinElTitular)));

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

        assertThrows(RecursoNoEncontradoException.class, () -> receptor.recibir(aOtroApartamento));

        assertEquals(ResultadoEvento.RECHAZADO, escenario.eventos.registrados.getFirst().resultado());
    }

    @Test
    @Tag("BIT-06")
    void siLaBitacoraFallaElCanalRecibeIgualSuRespuesta() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> receptor.recibir(pedido("CAN-99", "BK-4", conAna())));

        assertTrue(escenario.eventos.registrados.isEmpty());
    }
}
