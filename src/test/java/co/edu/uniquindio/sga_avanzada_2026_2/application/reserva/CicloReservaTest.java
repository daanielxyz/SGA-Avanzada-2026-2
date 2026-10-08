package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

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
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NoShowDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ciclo de la reserva en la aplicación, con repositorios en memoria. Hoy es el 1 de octubre de 2026 a las 10:00
 * ({@link RepositoriosEnMemoria#RELOJ}); la tarifa base es de 100.000 por ocupante facturable y noche.
 */
class CicloReservaTest {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    private static final ApartamentoId APT_101 = new ApartamentoId("APT-101");
    private static final ApartamentoId APT_102 = new ApartamentoId("APT-102");
    private static final LocalDate ENTRADA = LocalDate.of(2026, 10, 10);
    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private RepositoriosEnMemoria.Reservas reservas;
    private RepositoriosEnMemoria.Folios folios;
    private RepositoriosEnMemoria.Titulares titulares;
    private RepositoriosEnMemoria.Apartamentos apartamentos;
    private RepositoriosEnMemoria.Alojamientos alojamientos;
    private RepositoriosEnMemoria.Calendarios calendarios;
    private RepositoriosEnMemoria.Tarifas tarifas;
    private RepositoriosEnMemoria.Politicas politicas;
    private RepositoriosEnMemoria.Codigos codigos;
    private CargadorReserva cargador;

    @BeforeEach
    void preparar() {
        reservas = new RepositoriosEnMemoria.Reservas();
        folios = new RepositoriosEnMemoria.Folios();
        titulares = new RepositoriosEnMemoria.Titulares();
        apartamentos = new RepositoriosEnMemoria.Apartamentos();
        alojamientos = new RepositoriosEnMemoria.Alojamientos();
        calendarios = new RepositoriosEnMemoria.Calendarios();
        tarifas = new RepositoriosEnMemoria.Tarifas();
        politicas = new RepositoriosEnMemoria.Politicas();
        codigos = new RepositoriosEnMemoria.Codigos();
        cargador = new CargadorReserva(reservas, folios, apartamentos, alojamientos, calendarios, titulares, codigos);

        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        calendarios.guardar(CalendarioTemporadas.crear(ALO, new TemporadaId("TEM-1"), "Base"));
        apartamentos.guardar(apartamento(APT_101, 4));
        apartamentos.guardar(apartamento(APT_102, 2));
        tarifas.guardar(Tarifa.crear(new TarifaId("TAR-1"), APT_101, new TemporadaId("TEM-1"), Dinero.de(100_000),
                HOY));
        tarifas.guardar(Tarifa.crear(new TarifaId("TAR-2"), APT_102, new TemporadaId("TEM-1"), Dinero.de(80_000),
                HOY));
        politicas.guardar(PoliticaCancelacion.crear(new PoliticaId("POL-1"), ALO,
                List.of(new TramoCancelacion(48, delTotal(0)), new TramoCancelacion(0, delTotal(50))), delTotal(100),
                2));
    }

    private static Penalizacion delTotal(int porcentaje) {
        return Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(porcentaje));
    }

    private static Apartamento apartamento(ApartamentoId codigo, int capacidad) {
        return new Apartamento(codigo, ALO, "Apto " + codigo.valor(), null, new Capacidad(capacidad),
                new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), true);
    }

    private static Clock relojEn(LocalDateTime momento) {
        return Clock.fixed(momento.atZone(COLOMBIA).toInstant(), COLOMBIA);
    }

    private static final TitularCommand ANA = new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null);

    private static OcupanteCommand ana() {
        return new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", "Colombiana");
    }

    private static OcupanteCommand adulto() {
        return new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null);
    }

    private static CrearReservaCommand comando(String apartamento, int noches, List<OcupanteCommand> ocupantes) {
        return new CrearReservaCommand(apartamento, ENTRADA, ENTRADA.plusDays(noches), "DIRECTO", ANA, ocupantes,
                null);
    }

    private CrearReserva crearReserva() {
        return new CrearReserva(cargador, reservas, folios, titulares, tarifas, politicas,
                new DisponibilidadDomainService(), new TarificacionDomainService(), codigos,
                RepositoriosEnMemoria.RELOJ);
    }

    private ReservaResult reservaDeDosAdultosPorTresNoches() {
        return crearReserva().ejecutar(comando("APT-101", 3, List.of(ana(), adulto())));
    }

    private Folio folio(String codigo) {
        return folios.buscarPorReserva(new ReservaId(codigo)).orElseThrow();
    }

    @Test
    @Tag("RN-05")
    @Tag("FOL-01")
    @Tag("RN-22")
    void deberiaCrearLaReservaPendienteConSuFolioYSuTitular() {
        ReservaResult reserva = reservaDeDosAdultosPorTresNoches();

        assertEquals("RES-2026-00001", reserva.codigo());
        assertEquals("PENDIENTE", reserva.estado());
        assertEquals(0, BigDecimal.valueOf(600_000).compareTo(reserva.valor().total()));
        assertEquals(3, reserva.valor().desglose().size());
        assertEquals("POL-1", reserva.politicaId());
        assertEquals("TIT-1", reserva.titularId());
        assertEquals(List.of("OCU-1", "OCU-2"), reserva.ocupantes().stream().map(ReservaResult.OcupanteResult::id)
                .toList());
        assertEquals(Dinero.de(600_000), folio(reserva.codigo()).saldo().monto());
        assertTrue(titulares.buscarPorId(new TitularId("TIT-1")).isPresent());
    }

    @Test
    @Tag("TIT-05")
    void unTitularQueYaExisteSeReutilizaConSusDatosActualizados() {
        titulares.guardar(new Titular(new TitularId("TIT-7"), ALO, "Ana R.", new Documento(TipoDocumento.CC, "1094"),
                new Correo("vieja@correo.co"), null));

        ReservaResult reserva = reservaDeDosAdultosPorTresNoches();

        assertEquals("TIT-7", reserva.titularId());
        assertEquals(1, titulares.guardados.size());
        assertEquals("Ana Ríos", titulares.guardados.get(new TitularId("TIT-7")).nombre());
        assertEquals(new Correo("ana@correo.co"), titulares.guardados.get(new TitularId("TIT-7")).correo());
    }

    @Test
    @Tag("RN-01")
    void noDeberiaVenderDosVecesLaMismaNocheNiDejarNadaGuardado() {
        reservaDeDosAdultosPorTresNoches();

        assertThrows(ReglaDominioException.class, () -> crearReserva().ejecutar(
                new CrearReservaCommand("APT-101", ENTRADA.plusDays(2), ENTRADA.plusDays(4), "PORTAL", ANA,
                        List.of(ana()), null)));
        assertEquals(1, reservas.guardadas.size());
        assertEquals(1, folios.guardados.size());
    }

    @Test
    @Tag("TIT-01")
    void elTitularDebeSerUnoDeLosOcupantes() {
        assertThrows(ReglaDominioException.class,
                () -> crearReserva().ejecutar(comando("APT-101", 3, List.of(adulto()))));
        assertTrue(reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("RN-02")
    void noDeberiaCrearUnaReservaQueExcedeLaCapacidad() {
        assertThrows(ReglaDominioException.class,
                () -> crearReserva().ejecutar(comando("APT-102", 2, List.of(ana(), adulto(),
                        new OcupanteCommand("Eva", LocalDate.of(2000, 1, 1), null, null, null)))));
    }

    @Test
    @Tag("CORI-03")
    void unaReservaExternaNoSeCreaPorEsteCasoDeUso() {
        assertThrows(ReglaDominioException.class, () -> crearReserva().ejecutar(new CrearReservaCommand("APT-101",
                ENTRADA, ENTRADA.plusDays(2), "EXTERNO", ANA, List.of(ana()), null)));
    }

    @Test
    void deberiaRechazarUnApartamentoInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> crearReserva().ejecutar(comando("APT-999", 2, List.of(ana()))));
    }

    @Test
    @Tag("RN-06")
    @Tag("COT-02")
    void laCotizacionSoloCobraLosOcupantesFacturables() {
        CotizarEstancia cotizar = new CotizarEstancia(cargador, tarifas, new TarificacionDomainService(),
                RepositoriosEnMemoria.RELOJ);

        CotizacionResult cotizacion = cotizar.ejecutar(new CotizarEstanciaCommand("APT-101", ENTRADA,
                ENTRADA.plusDays(3), List.of(LocalDate.of(1990, 1, 1), LocalDate.of(2021, 1, 1))));

        assertEquals(0, BigDecimal.valueOf(300_000).compareTo(cotizacion.total()));
        assertEquals(1, cotizacion.desglose().getFirst().ocupantesFacturables());
        assertTrue(reservas.guardadas.isEmpty());
    }

    @Test
    @Tag("DISP-02")
    @Tag("RN-01")
    void laBusquedaSoloOfreceApartamentosLibresDondeElGrupoCabe() {
        reservaDeDosAdultosPorTresNoches();
        BuscarDisponibles buscar = new BuscarDisponibles(cargador, apartamentos, reservas,
                new DisponibilidadDomainService());

        List<ApartamentoDisponibleResult> paraDos = buscar.ejecutar(
                new BuscarDisponiblesCommand("ALO-1", ENTRADA, ENTRADA.plusDays(2), 2));
        List<ApartamentoDisponibleResult> paraTres = buscar.ejecutar(
                new BuscarDisponiblesCommand("ALO-1", ENTRADA, ENTRADA.plusDays(2), 3));

        assertEquals(List.of("APT-102"), paraDos.stream().map(ApartamentoDisponibleResult::codigo).toList());
        assertTrue(paraTres.isEmpty());
    }

    private ModificarReserva modificarReserva() {
        return new ModificarReserva(cargador, reservas, folios, tarifas, new DisponibilidadDomainService(),
                new TarificacionDomainService(), RepositoriosEnMemoria.RELOJ);
    }

    @Test
    @Tag("RN-14")
    @Tag("CAR-06")
    void modificarRecalculaElValorYRegistraLaDiferenciaEnElFolio() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();

        ReservaResult modificada = modificarReserva().ejecutar(new ModificarReservaCommand(codigo, "APT-101",
                ENTRADA, ENTRADA.plusDays(4), List.of(ana(), adulto())));

        assertEquals(0, BigDecimal.valueOf(800_000).compareTo(modificada.valor().total()));
        assertEquals(Dinero.de(800_000), folio(codigo).saldo().monto());
    }

    @Test
    @Tag("RN-14")
    void cambiarDeApartamentoBajaElValorYLasNochesPropiasNoEstorban() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();

        ReservaResult enOtro = modificarReserva().ejecutar(new ModificarReservaCommand(codigo, "APT-102",
                ENTRADA, ENTRADA.plusDays(3), List.of(ana(), adulto())));
        ReservaResult deVuelta = modificarReserva().ejecutar(new ModificarReservaCommand(codigo, "APT-102",
                ENTRADA.plusDays(1), ENTRADA.plusDays(4), List.of(ana(), adulto())));

        assertEquals("APT-102", enOtro.apartamento());
        assertEquals(0, BigDecimal.valueOf(480_000).compareTo(deVuelta.valor().total()));
        assertEquals(Dinero.de(480_000), folio(codigo).saldo().monto());
    }

    @Test
    @Tag("RN-09")
    @Tag("RES-15")
    void confirmarExigeHoraDeLlegadaYElAnticipoPagado() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();
        ConfirmarReserva confirmar = new ConfirmarReserva(cargador, reservas);

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(codigo));
        new IndicarHoraLlegada(cargador, reservas).ejecutar(new IndicarHoraLlegadaCommand(codigo, LocalTime.of(16, 0)));
        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(codigo));

        folio(codigo).registrarPago(new MedioPago("EFECTIVO"), Dinero.de(180_000), HOY, HOY,
                List.of(new MedioPago("EFECTIVO")));

        assertEquals("CONFIRMADA", confirmar.ejecutar(codigo).estado());
    }

    private CancelarReserva cancelarEn(LocalDateTime momento) {
        return new CancelarReserva(cargador, reservas, folios, politicas, new CancelacionDomainService(),
                relojEn(momento));
    }

    @Test
    @Tag("RN-13")
    void cancelarConMasDe48HorasNoRetieneNada() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();

        CancelacionResult resultado = cancelarEn(HOY.atTime(10, 0)).ejecutar(codigo);

        assertEquals("CANCELADA", resultado.estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.retenido()));
        assertEquals(SituacionSaldo.AL_DIA.name(), resultado.situacionSaldo());
    }

    @Test
    @Tag("RN-13")
    void cancelarDentroDeLas48HorasRetieneLaMitad() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();

        CancelacionResult resultado = cancelarEn(ENTRADA.minusDays(1).atTime(10, 0)).ejecutar(codigo);

        assertEquals(0, BigDecimal.valueOf(300_000).compareTo(resultado.retenido()));
        assertEquals(0, BigDecimal.valueOf(300_000).compareTo(resultado.saldo()));
        assertEquals(SituacionSaldo.PENDIENTE.name(), resultado.situacionSaldo());
    }

    @Test
    @Tag("RES-16")
    @Tag("POL-06")
    void elNoShowSeDeclaraDesdeLaHoraLimiteYRetieneSegunLaPolitica() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();
        new IndicarHoraLlegada(cargador, reservas).ejecutar(new IndicarHoraLlegadaCommand(codigo, LocalTime.of(16, 0)));
        folio(codigo).registrarPago(new MedioPago("EFECTIVO"), Dinero.de(180_000), HOY, HOY,
                List.of(new MedioPago("EFECTIVO")));
        new ConfirmarReserva(cargador, reservas).ejecutar(codigo);

        assertThrows(ReglaDominioException.class, () -> noShowEn(ENTRADA.atTime(21, 0)).ejecutar(codigo));
        CancelacionResult resultado = noShowEn(ENTRADA.atTime(22, 30)).ejecutar(codigo);

        assertEquals("NO_SHOW", resultado.estado());
        assertEquals(0, BigDecimal.valueOf(600_000).compareTo(resultado.retenido()));
        assertEquals(0, BigDecimal.valueOf(420_000).compareTo(resultado.saldo()));
    }

    private DeclararNoShow noShowEn(LocalDateTime momento) {
        return new DeclararNoShow(cargador, reservas, folios, politicas, new NoShowDomainService(),
                relojEn(momento));
    }

    @Test
    @Tag("RN-21")
    void unaPendienteVenceSoloDespuesDelPlazoYSinPenalidad() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();
        LocalDateTime plazoCumplido = HOY.atTime(10, 0).plusHours(24);

        assertTrue(new BuscarReservasVencidas(cargador, reservas, relojEn(plazoCumplido)).ejecutar("ALO-1")
                .isEmpty());
        List<String> vencidas = new BuscarReservasVencidas(cargador, reservas, relojEn(plazoCumplido.plusMinutes(1)))
                .ejecutar("ALO-1");
        CancelacionResult resultado = new VencerReserva(cargador, reservas, folios, new CancelacionDomainService(),
                relojEn(plazoCumplido.plusMinutes(1))).ejecutar(codigo);

        assertEquals(List.of(codigo), vencidas);
        assertEquals("CANCELADA", resultado.estado());
        assertEquals(SituacionSaldo.AL_DIA.name(), resultado.situacionSaldo());
    }

    @Test
    void consultarDevuelveLaReservaYRechazaUnaInexistente() {
        String codigo = reservaDeDosAdultosPorTresNoches().codigo();
        ConsultarReserva consultar = new ConsultarReserva(cargador);

        assertEquals(codigo, consultar.ejecutar(codigo).codigo());
        assertThrows(RecursoNoEncontradoException.class, () -> consultar.ejecutar("RES-2026-09999"));
    }
}
