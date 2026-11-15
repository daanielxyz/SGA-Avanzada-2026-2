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
import java.util.Optional;
import java.util.stream.IntStream;

import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.ADMIN;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.AL_CREAR;
import static co.edu.uniquindio.sga_avanzada_2026_2.application.EscenarioEstancia.relojEn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private RegistrarNovedadUseCase registrar(Clock reloj) {
        return new RegistrarNovedadUseCase(escenario.novedades, escenario.apartamentos, escenario.codigos, reloj);
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
        AvanzarNovedadUseCase avanzar = new AvanzarNovedadUseCase(escenario.novedades, relojEn(AHORA.plusHours(2)));

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
        ConsultarNovedadesUseCase consultar = new ConsultarNovedadesUseCase(escenario.novedades,
                escenario.apartamentos);

        Pagina<NovedadResult> primera = consultar.ejecutar("APT-101", 0);
        Pagina<NovedadResult> segunda = consultar.ejecutar("APT-101", 1);

        assertEquals(12, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(10, primera.contenido().size());
        assertEquals("NOV-12", primera.contenido().getFirst().id());
        assertEquals(List.of("NOV-2", "NOV-1"), segunda.contenido().stream().map(NovedadResult::id).toList());
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("APT-999", 0));
    }

    @Test
    @Tag("DEC-132")
    void laListaGeneralFiltraPorEstadoGravedadYApartamentoDeLaMasRecienteALaMasAntigua() {
        registrar(relojEn(AHORA.plusHours(1))).ejecutar(grifo());
        registrar(relojEn(AHORA.plusHours(2)))
                .ejecutar(new RegistrarNovedadCommand("APT-101", "Vidrio roto", "ALTA", SERVICIO));
        registrar(relojEn(AHORA.plusHours(3)))
                .ejecutar(new RegistrarNovedadCommand("APT-101", "Falta una toalla", "BAJA", SERVICIO));
        new AvanzarNovedadUseCase(escenario.novedades, relojEn(AHORA.plusHours(4)))
                .ejecutar(new AvanzarNovedadCommand("NOV-2", ADMIN));
        ListarNovedadesUseCase listar = new ListarNovedadesUseCase(escenario.novedades);

        assertEquals(List.of("NOV-3", "NOV-2", "NOV-1"), ids(listar.ejecutar(
                new ListarNovedadesCommand(null, null, null, 0))));
        assertEquals(List.of("NOV-3", "NOV-1"), ids(listar.ejecutar(
                new ListarNovedadesCommand(List.of("ABIERTA"), null, null, 0))));
        assertEquals(List.of("NOV-2", "NOV-1"), ids(listar.ejecutar(
                new ListarNovedadesCommand(List.of("ABIERTA", "EN_REVISION"), List.of("ALTA", "MEDIA"),
                        List.of("APT-101"), 0))));
        assertTrue(listar.ejecutar(new ListarNovedadesCommand(null, null, List.of("APT-102"), 0))
                .contenido().isEmpty());
    }

    @Test
    @Tag("DEC-132")
    void conNombresPoneElDeQuienDioCadaPasoYNadaSiEsaCuentaYaNoExiste() {
        NovedadResult novedad = registrar(AL_CREAR).ejecutar(grifo());

        NovedadResult conNombres = novedad.conNombres(id -> id.valor().equals(SERVICIO)
                ? Optional.of("Luz Marina Cardona") : Optional.empty());
        NovedadResult sinCuenta = novedad.conNombres(id -> Optional.empty());

        assertEquals("Luz Marina Cardona", conNombres.historial().getFirst().autorNombre());
        assertEquals(SERVICIO, conNombres.historial().getFirst().autor());
        assertNull(sinCuenta.historial().getFirst().autorNombre());
    }

    private static List<String> ids(Pagina<NovedadResult> pagina) {
        return pagina.contenido().stream().map(NovedadResult::id).toList();
    }}
