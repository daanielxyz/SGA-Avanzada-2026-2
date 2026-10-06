package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);
    private static final LocalDateTime AHORA = HOY.atTime(10, 0);
    private static final Estancia ESTANCIA = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final PoliticaId POLITICA = new PoliticaId("POL-1");
    private static final Capacidad CAPACIDAD = new Capacidad(4);
    private static final LocalTime HORA_LIMITE_NO_SHOW = LocalTime.of(22, 0);
    private static final Duration PLAZO = Duration.ofHours(24);
    private static final UsuarioId RECEPCION = new UsuarioId("USR-1");

    private static final Ocupante ANA = new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 3, 1),
            new Documento(TipoDocumento.CC, "1094"), null);
    private static final Ocupante LUIS = new Ocupante(new OcupanteId("OCU-2"), "Luis", LocalDate.of(1992, 7, 9),
            null, null);

    /** Cotización de 100.000 por ocupante y noche, con todos facturables. */
    private static Cotizacion cotizacion(Estancia estancia, int ocupantes) {
        List<LineaCotizacion> lineas = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-BASE"), Dinero.de(100_000), ocupantes,
                        Dinero.de(100_000L * ocupantes)))
                .toList();
        return new Cotizacion(lineas, Dinero.de(100_000L * ocupantes * estancia.cantidadNoches()));
    }

    private static Reserva crear(Estancia estancia, List<Ocupante> ocupantes, LocalTime horaEstimada,
                                 int estanciaMinima) {
        return Reserva.crear(new ReservaId("RES-2026-00001"), APT, new TitularId("TIT-1"), estancia,
                CanalOrigen.PORTAL, null, null, ocupantes, horaEstimada, cotizacion(estancia, ocupantes.size()),
                POLITICA, CAPACIDAD, estanciaMinima, AHORA);
    }

    private static Reserva pendiente() {
        return crear(ESTANCIA, List.of(ANA, LUIS), LocalTime.of(15, 0), 0);
    }

    private static Reserva confirmada() {
        Reserva reserva = pendiente();
        reserva.confirmar(new Porcentaje(0), Dinero.CERO);
        return reserva;
    }

    private static Reserva enCurso() {
        Reserva reserva = confirmada();
        reserva.registrarLlegada(new RegistroId("REG-1"), ESTANCIA.entrada().atTime(15, 30), RECEPCION);
        return reserva;
    }

    // --- Crear ---

    @Test
    @Tag("RN-04")
    void deberiaCrearReservaConFechaDeEntradaDeHoyOPosterior() {
        Estancia desdeHoy = new Estancia(HOY, HOY.plusDays(1));

        assertDoesNotThrow(() -> crear(desdeHoy, List.of(ANA), null, 0));
    }

    @Test
    @Tag("RN-04")
    void deberiaRechazarReservaConFechaDeEntradaAnteriorAHoy() {
        Estancia ayer = new Estancia(HOY.minusDays(1), HOY.plusDays(1));

        assertThrows(ReglaDominioException.class, () -> crear(ayer, List.of(ANA), null, 0));
    }

    @Test
    @Tag("RN-22")
    void deberiaCongelarValorYPoliticaVigenteAlCrearLaReserva() {
        Reserva reserva = pendiente();

        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());
        assertEquals(Dinero.de(400_000), reserva.valorTotal());
        assertEquals(2, reserva.desglose().size());
        assertEquals(POLITICA, reserva.politicaVersionId());
        assertEquals(AHORA, reserva.creadaEn());
        assertNull(reserva.registro());
        assertNull(reserva.salida());
    }

    @Test
    @Tag("RN-22")
    void noDeberiaCambiarElValorCongeladoSiLaTarifaCambiaDespues() {
        Reserva reserva = pendiente();
        Dinero congelado = reserva.valorTotal();

        reserva.confirmar(new Porcentaje(0), Dinero.CERO);
        reserva.registrarLlegada(new RegistroId("REG-1"), ESTANCIA.entrada().atTime(15, 0), RECEPCION);

        assertEquals(congelado, reserva.valorTotal());
    }

    @Test
    @Tag("RN-02")
    void deberiaAceptarOcupantesHastaLaCapacidadDelApartamento() {
        List<Ocupante> cuatro = List.of(ANA, LUIS, ocupante("OCU-3", "Eva"), ocupante("OCU-4", "Tom"));

        assertEquals(4, crear(ESTANCIA, cuatro, null, 0).ocupantes().size());
    }

    @Test
    @Tag("RN-02")
    void deberiaRechazarReservaSiExcedeCapacidadMaximaApartamento() {
        List<Ocupante> cinco = List.of(ANA, LUIS, ocupante("OCU-3", "Eva"), ocupante("OCU-4", "Tom"),
                ocupante("OCU-5", "Leo"));

        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, cinco, null, 0));
    }

    @Test
    @Tag("OCU-02")
    void deberiaRechazarReservaSinOcupantes() {
        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, List.of(), null, 0));
    }

    @Test
    @Tag("OCU-05")
    void deberiaRechazarOcupanteRepetidoPorDocumento() {
        Ocupante mismaPersona = new Ocupante(new OcupanteId("OCU-9"), "Ana María", LocalDate.of(1990, 3, 1),
                new Documento(TipoDocumento.CC, "1094"), null);

        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, List.of(ANA, mismaPersona), null, 0));
    }

    @Test
    @Tag("OCU-05")
    void deberiaRechazarOcupanteRepetidoPorNombreYFechaSinDocumento() {
        Ocupante mismaPersona = new Ocupante(new OcupanteId("OCU-9"), "LUIS", LUIS.fechaNacimiento(), null, null);

        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, List.of(LUIS, mismaPersona), null, 0));
    }

    @Test
    @Tag("OCU-12")
    void deberiaRechazarOcupanteNacidoDespuesDeHoy() {
        Ocupante futuro = new Ocupante(new OcupanteId("OCU-9"), "Bebé", HOY.plusDays(1), null, null);

        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, List.of(ANA, futuro), null, 0));
    }

    @Test
    @Tag("RP-01")
    void deberiaAceptarEstanciaQueCumpleElMinimoDeLaTemporada() {
        assertDoesNotThrow(() -> crear(ESTANCIA, List.of(ANA), null, 2));
    }

    @Test
    @Tag("RP-01")
    void deberiaRechazarEstanciaMasCortaQueElMinimoDeLaTemporada() {
        assertThrows(ReglaDominioException.class, () -> crear(ESTANCIA, List.of(ANA), null, 3));
    }

    @Test
    @Tag("CORI-03")
    void deberiaExigirCanalEIdentificadorExternoSoloEnReservasExternas() {
        assertDoesNotThrow(() -> externa(new CanalId("CAN-1"), "BK-77"));
        assertThrows(ReglaDominioException.class, () -> externa(null, null));
        assertThrows(ReglaDominioException.class, () -> Reserva.crear(new ReservaId("RES-2026-00001"), APT,
                new TitularId("TIT-1"), ESTANCIA, CanalOrigen.DIRECTO, new CanalId("CAN-1"), "BK-77", List.of(ANA),
                null, cotizacion(ESTANCIA, 1), POLITICA, CAPACIDAD, 0, AHORA));
    }

    @Test
    void deberiaRechazarCotizacionDeOtraEstancia() {
        Estancia otra = new Estancia(ESTANCIA.entrada(), ESTANCIA.salida().plusDays(1));

        assertThrows(ReglaDominioException.class, () -> Reserva.crear(new ReservaId("RES-2026-00001"), APT,
                new TitularId("TIT-1"), ESTANCIA, CanalOrigen.PORTAL, null, null, List.of(ANA), null,
                cotizacion(otra, 1), POLITICA, CAPACIDAD, 0, AHORA));
    }

    // --- Confirmar ---

    @Test
    @Tag("RN-09")
    void deberiaConfirmarReservaConHoraEstimadaDeLlegada() {
        Reserva reserva = pendiente();

        reserva.confirmar(new Porcentaje(0), Dinero.CERO);

        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    @Test
    @Tag("RN-09")
    void deberiaRechazarConfirmarSinHoraEstimadaDeLlegada() {
        Reserva reserva = crear(ESTANCIA, List.of(ANA), null, 0);

        assertThrows(ReglaDominioException.class, () -> reserva.confirmar(new Porcentaje(0), Dinero.CERO));

        reserva.indicarHoraEstimadaLlegada(LocalTime.of(18, 0));
        reserva.confirmar(new Porcentaje(0), Dinero.CERO);
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    @Test
    @Tag("RES-15")
    void deberiaRechazarConfirmarSiElAnticipoEsMenorAlPorcentaje() {
        Reserva reserva = pendiente(); // 400.000 → 30 % = 120.000

        assertThrows(ReglaDominioException.class, () -> reserva.confirmar(new Porcentaje(30), Dinero.de(119_999)));
        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());
    }

    @Test
    @Tag("RES-15")
    void deberiaConfirmarSiElPagoCubreElAnticipo() {
        Reserva reserva = pendiente();

        reserva.confirmar(new Porcentaje(30), Dinero.de(120_000));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    // --- Ciclo de vida ---

    @Test
    @Tag("RN-08")
    void deberiaPermitirLasTransicionesDelCicloDeVida() {
        Reserva reserva = enCurso();
        assertEquals(EstadoReserva.EN_CURSO, reserva.estado());

        reserva.registrarSalida(new SalidaId("SAL-1"), LocalDate.of(2026, 12, 12).atTime(10, 0), RECEPCION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
        assertEquals(RECEPCION, reserva.salida().autor());
    }

    @Test
    @Tag("RN-08")
    void deberiaRechazarTransicionNoListadaDesdeEstadoTerminal() {
        Reserva reserva = pendiente();
        reserva.cancelar();

        assertThrows(ReglaDominioException.class, () -> reserva.confirmar(new Porcentaje(0), Dinero.CERO));
        assertThrows(ReglaDominioException.class, reserva::cancelar);
        assertThrows(ReglaDominioException.class, () -> reserva.indicarHoraEstimadaLlegada(LocalTime.NOON));
        assertEquals(EstadoReserva.CANCELADA, reserva.estado());
    }

    @Test
    @Tag("RN-08")
    void noDeberiaCancelarUnaReservaEnCurso() {
        Reserva reserva = enCurso();

        assertThrows(ReglaDominioException.class, reserva::cancelar);
    }

    @Test
    @Tag("RN-12")
    void deberiaLiberarLasNochesAlCancelarReserva() {
        Reserva reserva = confirmada();

        reserva.cancelar();

        assertFalse(reserva.estaActiva());
        assertFalse(reserva.retieneNochesDe(ESTANCIA));
    }

    @Test
    @Tag("RN-12")
    void deberiaMantenerRetenidasLasNochesMientrasLaReservaEstaActiva() {
        assertTrue(pendiente().retieneNochesDe(ESTANCIA));
        assertTrue(confirmada().retieneNochesDe(ESTANCIA));
        assertTrue(enCurso().retieneNochesDe(ESTANCIA));
        assertFalse(pendiente().retieneNochesDe(new Estancia(ESTANCIA.salida(), ESTANCIA.salida().plusDays(2))));
    }

    // --- No-show y vencimiento ---

    @Test
    @Tag("RES-16")
    void deberiaDeclararNoShowDesdeLaHoraLimiteDelDiaDeEntrada() {
        Reserva reserva = confirmada();

        reserva.declararNoShow(ESTANCIA.entrada().atTime(HORA_LIMITE_NO_SHOW), HORA_LIMITE_NO_SHOW);

        assertEquals(EstadoReserva.NO_SHOW, reserva.estado());
        assertFalse(reserva.estaActiva());
    }

    @Test
    @Tag("RES-16")
    void deberiaRechazarNoShowAntesDeLaHoraLimiteOSinConfirmar() {
        Reserva reserva = confirmada();
        LocalDateTime antes = ESTANCIA.entrada().atTime(HORA_LIMITE_NO_SHOW).minusMinutes(1);

        assertThrows(ReglaDominioException.class, () -> reserva.declararNoShow(antes, HORA_LIMITE_NO_SHOW));
        assertThrows(ReglaDominioException.class, () -> pendiente()
                .declararNoShow(ESTANCIA.entrada().atTime(23, 0), HORA_LIMITE_NO_SHOW));
    }

    @Test
    @Tag("RN-21")
    void siReservaPendienteSuperaPlazoSeCancela() {
        Reserva reserva = pendiente();

        reserva.expirar(AHORA.plus(PLAZO).plusMinutes(1), PLAZO);

        assertEquals(EstadoReserva.CANCELADA, reserva.estado());
    }

    @Test
    @Tag("RN-21")
    void noDeberiaCancelarReservaPendienteQueNoSuperaElPlazo() {
        Reserva reserva = pendiente();

        assertThrows(ReglaDominioException.class, () -> reserva.expirar(AHORA.plus(PLAZO), PLAZO));
        assertThrows(ReglaDominioException.class, () -> confirmada().expirar(AHORA.plusDays(5), PLAZO));
        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());
    }

    // --- Llegada y salida ---

    @Test
    @Tag("RN-10")
    void deberiaRegistrarLlegadaConReservaConfirmadaYFechaAlcanzada() {
        Reserva reserva = confirmada();
        LocalDateTime llegada = ESTANCIA.entrada().atTime(16, 0);

        reserva.registrarLlegada(new RegistroId("REG-1"), llegada, RECEPCION);

        assertEquals(EstadoReserva.EN_CURSO, reserva.estado());
        assertEquals(llegada, reserva.registro().fechaHora());
        assertEquals(RECEPCION, reserva.registro().autor());
        assertFalse(reserva.registro().anulado());
    }

    @Test
    @Tag("RN-10")
    void noPermitirRegistrarLlegadaSinEstadoConfirmado() {
        Reserva reserva = pendiente();

        assertThrows(ReglaDominioException.class,
                () -> reserva.registrarLlegada(new RegistroId("REG-1"), ESTANCIA.entrada().atTime(16, 0), RECEPCION));
    }

    @Test
    @Tag("RN-10")
    void noPermitirRegistrarLlegadaAntesDeLaFechaDeEntrada() {
        Reserva reserva = confirmada();
        LocalDateTime laVispera = ESTANCIA.entrada().minusDays(1).atTime(23, 59);

        assertThrows(ReglaDominioException.class,
                () -> reserva.registrarLlegada(new RegistroId("REG-1"), laVispera, RECEPCION));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    @Test
    @Tag("SAL-01")
    void noDeberiaRegistrarSalidaSinEstarEnCurso() {
        Reserva reserva = confirmada();

        assertThrows(ReglaDominioException.class,
                () -> reserva.registrarSalida(new SalidaId("SAL-1"), ESTANCIA.salida().atTime(10, 0), RECEPCION));
    }

    @Test
    @Tag("SAL-05")
    void noDeberiaRegistrarSalidaAnteriorAlRegistro() {
        Reserva reserva = enCurso(); // llegó a las 15:30 del día de entrada

        assertThrows(ReglaDominioException.class,
                () -> reserva.registrarSalida(new SalidaId("SAL-1"), ESTANCIA.entrada().atTime(15, 0), RECEPCION));
    }

    @Test
    @Tag("SAL-04")
    void unaSalidaAnticipadaFinalizaLaReservaSinCancelarla() {
        Reserva reserva = enCurso();

        reserva.registrarSalida(new SalidaId("SAL-1"), ESTANCIA.entrada().atTime(20, 0), RECEPCION);

        assertEquals(EstadoReserva.FINALIZADA, reserva.estado());
    }

    // --- Modificar ---

    @Test
    @Tag("RN-14")
    void deberiaRecalcularElValorYRegistrarAjusteAlModificarReserva() {
        Reserva reserva = confirmada();
        Estancia masLarga = new Estancia(ESTANCIA.entrada(), ESTANCIA.salida().plusDays(1));
        ApartamentoId otro = new ApartamentoId("APT-102");

        reserva.modificar(masLarga, List.of(ANA), otro, cotizacion(masLarga, 1), CAPACIDAD, 0, HOY);

        assertEquals(masLarga, reserva.estancia());
        assertEquals(otro, reserva.apartamentoId());
        assertEquals(Dinero.de(300_000), reserva.valorTotal());
        assertEquals(3, reserva.desglose().size());
        assertEquals(POLITICA, reserva.politicaVersionId());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
    }

    @Test
    @Tag("RN-14")
    void deberiaRechazarModificacionSiLaNuevaCondicionNoEsValida() {
        Reserva reserva = pendiente();
        List<Ocupante> cinco = List.of(ANA, LUIS, ocupante("OCU-3", "Eva"), ocupante("OCU-4", "Tom"),
                ocupante("OCU-5", "Leo"));

        assertThrows(ReglaDominioException.class, () -> reserva.modificar(ESTANCIA, cinco, APT,
                cotizacion(ESTANCIA, 5), CAPACIDAD, 0, HOY));
        assertThrows(ReglaDominioException.class, () -> reserva.modificar(ESTANCIA, List.of(ANA), APT,
                cotizacion(ESTANCIA, 1), CAPACIDAD, 3, HOY));
        assertEquals(Dinero.de(400_000), reserva.valorTotal());
        assertEquals(2, reserva.ocupantes().size());
    }

    @Test
    @Tag("RN-14")
    void noDeberiaModificarUnaReservaQueYaInicio() {
        Reserva reserva = enCurso();

        assertThrows(ReglaDominioException.class, () -> reserva.modificar(ESTANCIA, List.of(ANA), APT,
                cotizacion(ESTANCIA, 1), CAPACIDAD, 0, ESTANCIA.entrada()));
    }

    // --- Reconstrucción ---

    @Test
    void deberiaRechazarReconstruirConRegistroQueNoCorrespondeAlEstado() {
        Cotizacion cotizacion = cotizacion(ESTANCIA, 1);

        assertThrows(ReglaDominioException.class, () -> new Reserva(new ReservaId("RES-2026-00001"), APT,
                new TitularId("TIT-1"), ESTANCIA, EstadoReserva.EN_CURSO, CanalOrigen.PORTAL, null, null,
                List.of(ANA), null, null, null, cotizacion.total(), cotizacion.desglose(), POLITICA, AHORA));
    }

    private static Reserva externa(CanalId canal, String idExterno) {
        return Reserva.crear(new ReservaId("RES-2026-00001"), APT, new TitularId("TIT-1"), ESTANCIA,
                CanalOrigen.EXTERNO, canal, idExterno, List.of(ANA), null, cotizacion(ESTANCIA, 1), POLITICA,
                CAPACIDAD, 0, AHORA);
    }

    private static Ocupante ocupante(String id, String nombre) {
        return new Ocupante(new OcupanteId(id), nombre, LocalDate.of(2000, 1, 1), null, null);
    }
}
