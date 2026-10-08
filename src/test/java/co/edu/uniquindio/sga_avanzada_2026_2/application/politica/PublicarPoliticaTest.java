package co.edu.uniquindio.sga_avanzada_2026_2.application.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.PenalizacionCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.TramoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicarPoliticaTest {

    private RepositoriosEnMemoria.Politicas politicas;
    private PublicarPolitica publicar;
    private ConsultarPoliticaVigente consultar;

    @BeforeEach
    void preparar() {
        politicas = new RepositoriosEnMemoria.Politicas();
        RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        publicar = new PublicarPolitica(politicas, alojamientos, new RepositoriosEnMemoria.Codigos());
        consultar = new ConsultarPoliticaVigente(politicas);
    }

    private static PublicarPoliticaCommand comando(String alojamientoId, List<TramoCommand> tramos) {
        return new PublicarPoliticaCommand(alojamientoId, tramos, new PenalizacionCommand("PAGADO", 100, null));
    }

    private static List<TramoCommand> gratisHasta48hLuegoMontoFijo() {
        return List.of(new TramoCommand(48, new PenalizacionCommand("VALOR_TOTAL", 0, null)),
                new TramoCommand(0, new PenalizacionCommand("ANTICIPO_EXIGIDO", null, BigDecimal.valueOf(50_000))));
    }

    @Test
    @Tag("POL-06")
    void laPrimeraPublicacionCreaLaVersionUno() {
        PoliticaResult resultado = publicar.ejecutar(comando("ALO-1", gratisHasta48hLuegoMontoFijo()));

        assertEquals(1, resultado.version());
        assertEquals(0, resultado.tramos().getFirst().antelacionMinHoras());
        assertEquals(0, BigDecimal.valueOf(50_000).compareTo(resultado.tramos().getFirst().penalizacion().montoFijo()));
        assertNull(resultado.tramos().getFirst().penalizacion().porcentaje());
        assertEquals("PAGADO", resultado.penalizacionNoShow().base());
    }

    @Test
    @Tag("POL-04")
    void cadaPublicacionCreaLaVersionSiguienteYConservaLaAnterior() {
        PoliticaResult primera = publicar.ejecutar(comando("ALO-1", gratisHasta48hLuegoMontoFijo()));

        PoliticaResult segunda = publicar.ejecutar(comando("ALO-1", List.of(
                new TramoCommand(72, new PenalizacionCommand("VALOR_TOTAL", 10, null)),
                new TramoCommand(0, new PenalizacionCommand("VALOR_TOTAL", 60, null)))));

        assertEquals(2, segunda.version());
        assertEquals(segunda, consultar.ejecutar("ALO-1"));
        assertTrue(politicas.buscarPorId(new PoliticaId(primera.id())).isPresent());
    }

    @Test
    @Tag("POL-01")
    void noDeberiaPublicarConMenosTramosQueElMinimo() {
        assertThrows(ReglaDominioException.class, () -> publicar.ejecutar(comando("ALO-1",
                List.of(new TramoCommand(0, new PenalizacionCommand("VALOR_TOTAL", 50, null))))));
        assertTrue(politicas.guardadas.isEmpty());
    }

    @Test
    @Tag("POL-03")
    void noDeberiaAceptarUnaPenalizacionConPorcentajeYMontoFijo() {
        assertThrows(ReglaDominioException.class, () -> publicar.ejecutar(comando("ALO-1", List.of(
                new TramoCommand(48, new PenalizacionCommand("VALOR_TOTAL", 0, null)),
                new TramoCommand(0, new PenalizacionCommand("VALOR_TOTAL", 50, BigDecimal.TEN))))));
    }

    @Test
    void deberiaRechazarUnAlojamientoSinPoliticaOInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("ALO-1"));
        assertThrows(RecursoNoEncontradoException.class,
                () -> publicar.ejecutar(comando("ALO-9", gratisHasta48hLuegoMontoFijo())));
    }
}
