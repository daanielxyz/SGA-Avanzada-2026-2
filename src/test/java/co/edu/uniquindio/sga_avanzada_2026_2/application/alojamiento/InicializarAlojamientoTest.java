package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.InicializarAlojamientoCommand.ServicioCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.PenalizacionCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand.TramoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InicializarAlojamientoTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private RepositoriosEnMemoria.Alojamientos alojamientos;
    private RepositoriosEnMemoria.Politicas politicas;
    private RepositoriosEnMemoria.Calendarios calendarios;
    private InicializarAlojamiento inicializar;

    @BeforeEach
    void preparar() {
        alojamientos = new RepositoriosEnMemoria.Alojamientos();
        politicas = new RepositoriosEnMemoria.Politicas();
        calendarios = new RepositoriosEnMemoria.Calendarios();
        inicializar = new InicializarAlojamiento(alojamientos, politicas, calendarios,
                new RepositoriosEnMemoria.Codigos());
    }

    static ParametrosCommand parametros() {
        return new ParametrosCommand(12, LocalTime.of(15, 0), LocalTime.of(11, 0), 3, 24, LocalTime.of(22, 0), 30,
                2, 1, 2, 2, 2);
    }

    private static InicializarAlojamientoCommand comando(String nombre, List<String> medios) {
        PenalizacionCommand gratis = new PenalizacionCommand("VALOR_TOTAL", 0, null);
        PenalizacionCommand mitad = new PenalizacionCommand("VALOR_TOTAL", 50, null);
        return new InicializarAlojamientoCommand("ALO-1", nombre, "Por definir", "Por definir", "Por definir", 0, 0,
                "", parametros(), List.of(new ServicioCommand("Parqueadero", false, BigDecimal.ZERO)), medios,
                List.of(new TramoCommand(48, gratis), new TramoCommand(0, mitad)),
                new PenalizacionCommand("VALOR_TOTAL", 100, null), "Temporada base");
    }

    @Test
    @Tag("ALO-01")
    @Tag("TEM-08")
    void deberiaCrearElAlojamientoSuPoliticaYSuCalendario() {
        AlojamientoResult resultado = inicializar.ejecutar(comando("Puerta al Sol", List.of("EFECTIVO", "NEQUI")));

        assertEquals("ALO-1", resultado.id());
        assertEquals(List.of("EFECTIVO", "NEQUI"), resultado.mediosPago());
        assertEquals("SRV-1", resultado.serviciosAdicionales().getFirst().id());
        PoliticaCancelacion politica = politicas.buscarVigente(ALO).orElseThrow();
        assertEquals(1, politica.version());
        assertEquals(2, politica.tramos().size());
        CalendarioTemporadas calendario = calendarios.buscarPorAlojamiento(ALO).orElseThrow();
        assertTrue(calendario.temporadas().getFirst().esBase());
    }

    @Test
    @Tag("ALO-01")
    void siYaExisteNoDeberiaCambiarNada() {
        inicializar.ejecutar(comando("Puerta al Sol", List.of("EFECTIVO", "NEQUI")));

        AlojamientoResult resultado = inicializar.ejecutar(comando("Otro nombre", List.of("EFECTIVO", "NEQUI")));

        assertEquals("Puerta al Sol", resultado.nombre());
        assertEquals(1, politicas.guardadas.size());
        assertEquals(1, calendarios.guardados.get(ALO).temporadas().size());
    }

    @Test
    @Tag("ALO-06")
    void noDeberiaGuardarNadaSiLosValoresInicialesViolanUnaRegla() {
        assertThrows(ReglaDominioException.class,
                () -> inicializar.ejecutar(comando("Puerta al Sol", List.of("EFECTIVO"))));

        assertTrue(alojamientos.guardados.isEmpty());
        assertTrue(politicas.guardadas.isEmpty());
        assertTrue(calendarios.guardados.isEmpty());
    }
}
