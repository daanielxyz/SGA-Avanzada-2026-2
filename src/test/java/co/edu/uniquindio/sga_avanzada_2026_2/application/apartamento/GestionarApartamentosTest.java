package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.RepositoriosEnMemoria;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ActivadorApartamentoService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BajaApartamentoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BloqueoOperativoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CambioCapacidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Casos de uso con los que el administrador gestiona apartamentos (CU-31 · CU-32 · CU-40 · CU-42 · CU-52).
 */
class GestionarApartamentosTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    private static final ApartamentoId APT_101 = new ApartamentoId("APT-101");
    private static final ApartamentoId APT_102 = new ApartamentoId("APT-102");
    private static final TemporadaId BASE = new TemporadaId("TEM-1");
    private static final TemporadaId ALTA = new TemporadaId("TEM-2");
    private static final TemporadaId MEDIA = new TemporadaId("TEM-3");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private RepositoriosEnMemoria.Apartamentos apartamentos;
    private RepositoriosEnMemoria.Alojamientos alojamientos;
    private RepositoriosEnMemoria.Calendarios calendarios;
    private RepositoriosEnMemoria.Tarifas tarifas;
    private RepositoriosEnMemoria.Reservas reservas;

    @BeforeEach
    void preparar() {
        apartamentos = new RepositoriosEnMemoria.Apartamentos();
        alojamientos = new RepositoriosEnMemoria.Alojamientos();
        calendarios = new RepositoriosEnMemoria.Calendarios();
        tarifas = new RepositoriosEnMemoria.Tarifas();
        reservas = new RepositoriosEnMemoria.Reservas();
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        CalendarioTemporadas calendario = CalendarioTemporadas.crear(ALO, BASE, "Base");
        calendario.agregarTemporada(ALTA, "Alta", LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15), 0);
        calendario.agregarTemporada(MEDIA, "Media", LocalDate.of(2027, 6, 15), LocalDate.of(2027, 7, 15), 0);
        calendarios.guardar(calendario);
    }

    private static Apartamento apartamento(ApartamentoId codigo, int capacidad, boolean activo) {
        return new Apartamento(codigo, ALO, "Apto " + codigo.valor(), null, new Capacidad(capacidad),
                new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), activo);
    }

    private void tarifasCompletas(ApartamentoId codigo) {
        IntStream.rangeClosed(1, 3).forEach(i -> tarifas.guardar(Tarifa.crear(
                new TarifaId("TAR-" + codigo.valor() + "-" + i), codigo, new TemporadaId("TEM-" + i),
                Dinero.de(100_000), HOY)));
    }

    private static Reserva reserva(String codigo, ApartamentoId apartamento, LocalDate entrada, int personas) {
        Estancia estancia = new Estancia(entrada, entrada.plusDays(2));
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, BASE, Dinero.de(100_000), personas,
                        Dinero.de(100_000L * personas)))
                .toList();
        List<Ocupante> ocupantes = IntStream.rangeClosed(1, personas)
                .mapToObj(i -> new Ocupante(new OcupanteId("OCU-" + i), "Huésped " + i, LocalDate.of(1990, 1, i),
                        null, null))
                .toList();
        return new Reserva(new ReservaId(codigo), apartamento, new TitularId("TIT-1"), estancia,
                EstadoReserva.CONFIRMADA, CanalOrigen.DIRECTO, null, null, ocupantes, List.of(), null, null,
                Dinero.de(200_000L * personas), desglose, new PoliticaId("POL-1"), HOY.atStartOfDay());
    }

    private ActivarApartamento activar() {
        return new ActivarApartamento(apartamentos, alojamientos, calendarios, tarifas,
                new ActivadorApartamentoService(), RepositoriosEnMemoria.RELOJ);
    }

    private CambiarCapacidad cambiarCapacidad() {
        return new CambiarCapacidad(apartamentos, alojamientos, reservas, new CambioCapacidadDomainService());
    }

    @Test
    @Tag("APA-11")
    @Tag("TAR-03")
    void deberiaActivarUnApartamentoConTarifasEnTodasLasTemporadas() {
        apartamentos.guardar(apartamento(APT_101, 4, false));
        tarifasCompletas(APT_101);

        assertTrue(activar().ejecutar("APT-101").activo());
    }

    @Test
    @Tag("TAR-03")
    void noDeberiaActivarSiLeFaltaUnaTarifa() {
        apartamentos.guardar(apartamento(APT_101, 4, false));
        tarifas.guardar(Tarifa.crear(new TarifaId("TAR-1"), APT_101, BASE, Dinero.de(100_000), HOY));

        assertThrows(ReglaDominioException.class, () -> activar().ejecutar("APT-101"));
    }

    @Test
    @Tag("CAP-05")
    void noDeberiaActivarElSegundoApartamentoConLaMismaCapacidadQueElPrimero() {
        apartamentos.guardar(apartamento(APT_101, 4, true));
        apartamentos.guardar(apartamento(APT_102, 4, false));
        tarifasCompletas(APT_102);

        assertThrows(ReglaDominioException.class, () -> activar().ejecutar("APT-102"));
        assertFalse(apartamentos.guardados.get(APT_102).activo());
    }

    @Test
    @Tag("CAP-05")
    void unHotelQueNoExigeVariedadPuedeActivarCapacidadesIguales() {
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1", RepositoriosEnMemoria.parametros(1)));
        apartamentos.guardar(apartamento(APT_101, 4, true));
        apartamentos.guardar(apartamento(APT_102, 4, false));
        tarifasCompletas(APT_102);

        assertTrue(activar().ejecutar("APT-102").activo());
    }

    @Test
    void noDeberiaActivarUnApartamentoInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> activar().ejecutar("APT-999"));
    }

    @Test
    @Tag("APA-15")
    void deberiaCambiarLaCapacidadYAdvertirLasReservasQueYaNoCaben() {
        apartamentos.guardar(apartamento(APT_101, 4, false));
        reservas.guardar(reserva("RES-2026-00001", APT_101, LocalDate.of(2026, 11, 1), 4));
        reservas.guardar(reserva("RES-2026-00002", APT_101, LocalDate.of(2026, 11, 5), 2));

        CambioCapacidadResult resultado = cambiarCapacidad().ejecutar(new CambiarCapacidadCommand("APT-101", 3));

        assertEquals(3, resultado.apartamento().capacidad());
        assertEquals(List.of("RES-2026-00001"), resultado.reservasAfectadas());
    }

    @Test
    @Tag("CAP-05")
    void noDeberiaDejarIgualesLasCapacidadesDeLosActivos() {
        apartamentos.guardar(apartamento(APT_101, 4, true));
        apartamentos.guardar(apartamento(APT_102, 2, true));

        assertThrows(ReglaDominioException.class,
                () -> cambiarCapacidad().ejecutar(new CambiarCapacidadCommand("APT-101", 2)));
    }

    @Test
    @Tag("APA-16")
    void deberiaRetirarDeLaVentaSoloSinReservasActivas() {
        apartamentos.guardar(apartamento(APT_101, 4, true));
        apartamentos.guardar(apartamento(APT_102, 2, true));
        reservas.guardar(reserva("RES-2026-00001", APT_102, LocalDate.of(2026, 11, 1), 2));
        RetirarApartamento retirar = new RetirarApartamento(apartamentos, reservas,
                new BajaApartamentoDomainService());

        assertFalse(retirar.ejecutar("APT-101").activo());
        assertThrows(ReglaDominioException.class, () -> retirar.ejecutar("APT-102"));
    }

    @Test
    @Tag("EOPE-02")
    void deberiaAvanzarLaPreparacionYRechazarUnaTransicionInvalida() {
        apartamentos.guardar(Apartamento.crear(APT_101, ALO, "Apto 101", null, new Capacidad(4), new Dormitorio(2),
                List.of(), List.of(new Caracteristica("Balcón"))));
        CambiarEstadoOperativo cambiar = new CambiarEstadoOperativo(apartamentos);

        assertEquals("EN_PREPARACION",
                cambiar.ejecutar(new CambiarEstadoOperativoCommand("APT-101", "EN_PREPARACION")).estadoOperativo());
        assertThrows(ReglaDominioException.class,
                () -> cambiar.ejecutar(new CambiarEstadoOperativoCommand("APT-101", "OCUPADO")));
    }

    @Test
    @Tag("BLO-01")
    @Tag("BLO-06")
    void deberiaBloquearFechasLibresYLevantarElBloqueo() {
        apartamentos.guardar(apartamento(APT_101, 4, true));
        RegistrarBloqueo registrar = new RegistrarBloqueo(apartamentos, reservas,
                new BloqueoOperativoDomainService(), new RepositoriosEnMemoria.Codigos());

        ApartamentoResult bloqueado = registrar.ejecutar(new RegistrarBloqueoCommand("APT-101",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), "Pintura"));
        ApartamentoResult levantado = new LevantarBloqueo(apartamentos)
                .ejecutar(new LevantarBloqueoCommand("APT-101", "BLO-1"));

        assertEquals("BLO-1", bloqueado.bloqueos().getFirst().id());
        assertFalse(levantado.bloqueos().getFirst().vigente());
    }

    @Test
    @Tag("BLO-01")
    void noDeberiaBloquearNochesDeUnaReservaActiva() {
        apartamentos.guardar(apartamento(APT_101, 4, true));
        reservas.guardar(reserva("RES-2026-00001", APT_101, LocalDate.of(2026, 11, 1), 2));
        RegistrarBloqueo registrar = new RegistrarBloqueo(apartamentos, reservas,
                new BloqueoOperativoDomainService(), new RepositoriosEnMemoria.Codigos());

        assertThrows(ReglaDominioException.class, () -> registrar.ejecutar(new RegistrarBloqueoCommand("APT-101",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), "Pintura")));
        assertTrue(apartamentos.guardados.get(APT_101).bloqueos().isEmpty());
    }

    @Test
    void deberiaListarDeDiezEnDiezOrdenadosPorCodigo() {
        IntStream.rangeClosed(1, 12)
                .forEach(i -> apartamentos.guardar(apartamento(new ApartamentoId("APT-" + (100 + i)), 2, false)));

        Pagina<ApartamentoResult> segunda = new ListarApartamentos(apartamentos).ejecutar("ALO-1", 1);

        assertEquals(12, segunda.totalElementos());
        assertEquals(2, segunda.totalPaginas());
        assertEquals(List.of("APT-111", "APT-112"),
                segunda.contenido().stream().map(ApartamentoResult::codigo).toList());
    }
}
