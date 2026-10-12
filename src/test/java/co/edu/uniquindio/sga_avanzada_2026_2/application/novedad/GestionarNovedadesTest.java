package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ADMIN;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.relojEn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos de uso de Novedad sobre el APT-101 de {@link EscenarioEstancia}, que está PREPARADO y sin bloqueos.
 */
class GestionarNovedadesTest {

    private static final String SERVICIO = "USR-servicio";
    private static final LocalDateTime AHORA = LocalDateTime.now(AL_CREAR);

    private EscenarioEstancia escenario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioEstancia();
    }

    private RegistrarNovedad registrar(Clock reloj) {
        return new RegistrarNovedad(escenario.novedades, escenario.apartamentos, escenario.codigos, reloj);
    }

    private static RegistrarNovedadCommand grifo() {
        return new RegistrarNovedadCommand("APT-101", "Grifo del baño gotea", "MEDIA", SERVICIO);
    }

    @Test
    @Tag("NOV-01")
    @Tag("NOV-03")
    @Tag("NOV-05")
    void deberiaRegistrarUnaNovedadAbiertaSinTocarElApartamento() {
        NovedadResult novedad = registrar(AL_CREAR).ejecutar(grifo());

        assertEquals("NOV-1", novedad.id());
        assertEquals("ABIERTA", novedad.estado());
        assertEquals("MEDIA", novedad.gravedad());
        assertEquals(SERVICIO, novedad.historial().getFirst().autor());
        assertEquals(AHORA, novedad.historial().getFirst().fechaHora());
        assertEquals(EstadoOperativo.PREPARADO, escenario.apartamento().estadoOperativo());
        assertTrue(escenario.apartamento().bloqueos().isEmpty());
    }

    @Test
    @Tag("NOV-01")
    void noDeberiaRegistrarUnaNovedadSinDescripcionNiDeUnApartamentoInexistente() {
        assertThrows(ReglaDominioException.class, () -> registrar(AL_CREAR)
                .ejecutar(new RegistrarNovedadCommand("APT-101", " ", "BAJA", SERVICIO)));
        assertThrows(RecursoNoEncontradoException.class, () -> registrar(AL_CREAR)
                .ejecutar(new RegistrarNovedadCommand("APT-999", "Vidrio roto", "ALTA", SERVICIO)));
        assertTrue(escenario.novedades.guardadas.isEmpty());
    }

    @Test
    @Tag("NOV-04")
    @Tag("NOV-06")
    void deberiaAvanzarLaNovedadHastaCerrarlaDejandoAutorYFechaDeCadaPaso() {
        registrar(AL_CREAR).ejecutar(grifo());
        AvanzarNovedad avanzar = new AvanzarNovedad(escenario.novedades, relojEn(AHORA.plusHours(2)));

        NovedadResult enRevision = avanzar.ejecutar(new AvanzarNovedadCommand("NOV-1", ADMIN));
        NovedadResult cerrada = avanzar.ejecutar(new AvanzarNovedadCommand("NOV-1", ADMIN));

        assertEquals("EN_REVISION", enRevision.estado());
        assertEquals("CERRADA", cerrada.estado());
        assertEquals(List.of("ABIERTA", "EN_REVISION", "CERRADA"),
                cerrada.historial().stream().map(NovedadResult.CambioResult::estado).toList());
        assertEquals(ADMIN, cerrada.historial().getLast().autor());
        assertEquals(AHORA.plusHours(2), cerrada.historial().getLast().fechaHora());
        assertThrows(ReglaDominioException.class, () -> avanzar.ejecutar(new AvanzarNovedadCommand("NOV-1", ADMIN)));
        assertThrows(RecursoNoEncontradoException.class,
                () -> avanzar.ejecutar(new AvanzarNovedadCommand("NOV-9", ADMIN)));
    }

    @Test
    @Tag("NOV-06")
    @Tag("DEC-18")
    void elHistorialDelApartamentoVaDeLaMasRecienteALaMasAntiguaDeDiezEnDiez() {
        IntStream.rangeClosed(1, 12).forEach(i -> registrar(relojEn(AHORA.plusHours(i))).ejecutar(grifo()));
        ConsultarNovedades consultar = new ConsultarNovedades(escenario.novedades, escenario.apartamentos);

        Pagina<NovedadResult> primera = consultar.ejecutar("APT-101", 0);
        Pagina<NovedadResult> segunda = consultar.ejecutar("APT-101", 1);

        assertEquals(12, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(10, primera.contenido().size());
        assertEquals("NOV-12", primera.contenido().getFirst().id());
        assertEquals(List.of("NOV-2", "NOV-1"), segunda.contenido().stream().map(NovedadResult::id).toList());
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("APT-999", 0));
    }
}
