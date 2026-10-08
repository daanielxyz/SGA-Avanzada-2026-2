package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaCommand.TarifaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ActivadorApartamentoService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos de uso del calendario de temporadas y de las tarifas (CU-33 · CU-34).
 */
class TemporadasYTarifasTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private RepositoriosEnMemoria.Apartamentos apartamentos;
    private RepositoriosEnMemoria.Calendarios calendarios;
    private RepositoriosEnMemoria.Tarifas tarifas;
    private RepositoriosEnMemoria.Codigos codigos;

    @BeforeEach
    void preparar() {
        apartamentos = new RepositoriosEnMemoria.Apartamentos();
        calendarios = new RepositoriosEnMemoria.Calendarios();
        tarifas = new RepositoriosEnMemoria.Tarifas();
        codigos = new RepositoriosEnMemoria.Codigos();
        calendarios.guardar(CalendarioTemporadas.crear(ALO, new TemporadaId(codigos.siguiente(SerieCodigo.TEMPORADA)),
                "Base"));
        apartamentos.guardar(apartamento("APT-101", true));
        apartamentos.guardar(apartamento("APT-102", false));
    }

    private static Apartamento apartamento(String codigo, boolean activo) {
        return new Apartamento(new ApartamentoId(codigo), ALO, "Apto", null, new Capacidad(4), new Dormitorio(2),
                EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), activo);
    }

    private AgregarTemporada agregarTemporada() {
        return new AgregarTemporada(calendarios, tarifas, apartamentos, new ActivadorApartamentoService(), codigos,
                RepositoriosEnMemoria.RELOJ);
    }

    private DefinirTarifa definirTarifa() {
        return new DefinirTarifa(tarifas, apartamentos, calendarios, codigos, RepositoriosEnMemoria.RELOJ);
    }

    private static AgregarTemporadaCommand alta(List<TarifaCommand> tarifas) {
        return new AgregarTemporadaCommand("ALO-1", "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15), 3,
                tarifas);
    }

    private static TarifaCommand tarifa(String apartamento, long valor) {
        return new TarifaCommand(apartamento, BigDecimal.valueOf(valor));
    }

    @Test
    @Tag("TAR-03")
    @Tag("TEM-07")
    void laTemporadaNaceConLaTarifaDeCadaApartamentoActivo() {
        TemporadaCreadaResult resultado = agregarTemporada().ejecutar(alta(List.of(tarifa("APT-101", 150_000))));

        assertEquals("TEM-2", resultado.temporada().id());
        assertEquals(3, resultado.temporada().estanciaMinimaNoches());
        assertEquals(1, resultado.tarifas().size());
        assertEquals(1, resultado.tarifas().getFirst().version());
        assertEquals(HOY, resultado.tarifas().getFirst().vigenteDesde());
        assertEquals(2, calendarios.guardados.get(ALO).temporadas().size());
    }

    @Test
    @Tag("TAR-03")
    void sinLaTarifaDeUnApartamentoActivoLaTemporadaNoSeCrea() {
        assertThrows(ReglaDominioException.class,
                () -> agregarTemporada().ejecutar(alta(List.of(tarifa("APT-102", 150_000)))));

        assertTrue(tarifas.guardadas.isEmpty());
    }

    @Test
    void deberiaRechazarUnaTarifaDeUnApartamentoInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> agregarTemporada()
                .ejecutar(alta(List.of(tarifa("APT-101", 150_000), tarifa("APT-999", 150_000)))));
    }

    @Test
    @Tag("TEM-02")
    void noDeberiaAgregarUnaTemporadaQueSeSolapa() {
        agregarTemporada().ejecutar(alta(List.of(tarifa("APT-101", 150_000))));

        assertThrows(ReglaDominioException.class, () -> agregarTemporada().ejecutar(new AgregarTemporadaCommand(
                "ALO-1", "Fin de año", LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2), 0,
                List.of(tarifa("APT-101", 200_000)))));
    }

    @Test
    @Tag("TEM-05")
    @Tag("TEM-06")
    void deberiaCambiarLasFechasYDesactivarUnaTemporada() {
        agregarTemporada().ejecutar(alta(List.of(tarifa("APT-101", 150_000))));

        CalendarioResult cambiado = new CambiarFechasTemporada(calendarios).ejecutar(
                new CambiarFechasTemporadaCommand("ALO-1", "TEM-2", LocalDate.of(2026, 12, 20),
                        LocalDate.of(2027, 1, 10)));
        CalendarioResult desactivado = new DesactivarTemporada(calendarios)
                .ejecutar(new DesactivarTemporadaCommand("ALO-1", "TEM-2"));

        assertEquals(LocalDate.of(2026, 12, 20), cambiado.temporadas().getLast().fechaInicio());
        assertFalse(desactivado.temporadas().getLast().activa());
        assertNull(new ConsultarCalendario(calendarios).ejecutar("ALO-1").temporadas().getFirst().fechaInicio());
    }

    @Test
    @Tag("TEM-10")
    void laBaseNoSePuedeDesactivar() {
        assertThrows(ReglaDominioException.class, () -> new DesactivarTemporada(calendarios)
                .ejecutar(new DesactivarTemporadaCommand("ALO-1", "TEM-1")));
    }

    @Test
    @Tag("TAR-05")
    void definirLaTarifaDosVecesCreaDosVersionesYConservaLaPrimera() {
        TarifaResult primera = definirTarifa()
                .ejecutar(new DefinirTarifaCommand("APT-102", "TEM-1", BigDecimal.valueOf(100_000)));
        TarifaResult segunda = definirTarifa()
                .ejecutar(new DefinirTarifaCommand("APT-102", "TEM-1", BigDecimal.valueOf(120_000)));

        assertEquals(1, primera.version());
        assertEquals(2, segunda.version());
        assertEquals(2, tarifas.guardadas.size());
        assertEquals(List.of(segunda),
                new ConsultarTarifas(tarifas, RepositoriosEnMemoria.RELOJ).ejecutar("APT-102"));
    }

    @Test
    @Tag("TAR-01")
    void noDeberiaDefinirTarifaEnUnaTemporadaInexistenteOInactiva() {
        agregarTemporada().ejecutar(alta(List.of(tarifa("APT-101", 150_000))));
        new DesactivarTemporada(calendarios).ejecutar(new DesactivarTemporadaCommand("ALO-1", "TEM-2"));

        assertThrows(ReglaDominioException.class, () -> definirTarifa()
                .ejecutar(new DefinirTarifaCommand("APT-102", "TEM-2", BigDecimal.valueOf(100_000))));
        assertThrows(ReglaDominioException.class, () -> definirTarifa()
                .ejecutar(new DefinirTarifaCommand("APT-102", "TEM-9", BigDecimal.valueOf(100_000))));
    }

    @Test
    @Tag("TAR-02")
    void noDeberiaDefinirUnaTarifaEnCero() {
        assertThrows(ReglaDominioException.class, () -> definirTarifa()
                .ejecutar(new DefinirTarifaCommand("APT-102", "TEM-1", BigDecimal.ZERO)));
    }
}
