package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EstadoConflicto;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamento;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoActivo;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Booking (canal externo CAN-1) pide APT-101 del 10 al 12 de diciembre con el identificador BK-77.
 */
class SincronizacionCanalDomainServiceTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private static final CanalId BOOKING = new CanalId("CAN-1");
    private static final String ID_EXTERNO = "BK-77";
    private static final Estancia PEDIDA = new Estancia(dia(10), dia(12));
    private static final LocalDateTime AHORA = dia(1).atTime(9, 0);
    private static final ConflictoId CONFLICTO = new ConflictoId("CON-1");

    private final SincronizacionCanalDomainService servicio = new SincronizacionCanalDomainService();
    private final DisponibilidadDomainService disponibilidad = new DisponibilidadDomainService();

    private static Canal booking(boolean activo) {
        return new Canal(BOOKING, ALO, "Booking", CanalOrigen.EXTERNO, "sga.canales.booking", activo);
    }

    private static Reserva externa(String codigo, CanalId canal, String idExterno) {
        List<LineaCotizacion> desglose = PEDIDA.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-BASE"), Dinero.de(100_000), 1,
                        Dinero.de(100_000)))
                .toList();
        return new Reserva(new ReservaId(codigo), APT, new TitularId("TIT-1"), PEDIDA, EstadoReserva.PENDIENTE,
                CanalOrigen.EXTERNO, canal, idExterno,
                List.of(new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 1, 1), null, null)),
                List.of(), null, null, Dinero.de(200_000), desglose, new PoliticaId("POL-1"), AHORA);
    }

    private Disponibilidad disponibilidadCon(List<Reserva> reservas) {
        return disponibilidad.verificarDisponibilidad(apartamentoActivo(), PEDIDA, 1, reservas, parametros(3));
    }

    private IntegracionExterna integrar(Canal canal, Optional<Reserva> yaRecibida, Disponibilidad disponible,
                                        Supplier<Reserva> crear) {
        return servicio.integrarReservaExterna(canal, ID_EXTERNO, yaRecibida, disponible, crear, CONFLICTO, AHORA);
    }

    @Test
    @Tag("RN-18")
    @Tag("CU-12")
    void deberiaCrearReservaExternaCuandoNoHayConflicto() {
        Reserva nueva = externa("RES-2026-00010", BOOKING, ID_EXTERNO);

        IntegracionExterna resultado = integrar(booking(true), Optional.empty(), disponibilidadCon(List.of()),
                () -> nueva);

        assertEquals(ResultadoEvento.EXITOSO, resultado.resultado());
        assertSame(nueva, resultado.reservaCreada().orElseThrow());
        assertTrue(resultado.conflictoRegistrado().isEmpty());
        assertEquals(EstadoReserva.PENDIENTE, nueva.estado());
    }

    @Test
    @Tag("RN-18")
    @Tag("CONF-02")
    void registrarConflictoCanalSinAfectarReservaActiva() {
        Reserva vigente = confirmada("RES-2026-00001", 11, 13);

        IntegracionExterna resultado = integrar(booking(true), Optional.empty(), disponibilidadCon(List.of(vigente)),
                () -> {
                    throw new AssertionError("No debe crear la reserva si hay conflicto");
                });

        assertEquals(ResultadoEvento.CONFLICTO, resultado.resultado());
        assertTrue(resultado.reservaCreada().isEmpty());
        var conflicto = resultado.conflictoRegistrado().orElseThrow();
        assertEquals(EstadoConflicto.PENDIENTE, conflicto.estado());
        assertEquals(ID_EXTERNO, conflicto.idExterno());
        assertEquals(PEDIDA, conflicto.estanciaSolicitada());
        assertTrue(conflicto.motivo().contains("RES-2026-00001"));
        assertEquals(EstadoReserva.CONFIRMADA, vigente.estado());
        assertTrue(vigente.estaActiva());
    }

    @Test
    @Tag("DEC-48")
    void unBloqueoTambienQuedaComoConflicto() {
        Bloqueo pintura = new Bloqueo(new BloqueoId("BLO-1"), dia(11), dia(12), "Pintura", true);
        Disponibilidad bloqueado = disponibilidad.verificarDisponibilidad(apartamento(true, List.of(pintura)),
                PEDIDA, 1, List.of(), parametros(3));

        IntegracionExterna resultado = integrar(booking(true), Optional.empty(), bloqueado, () -> null);

        assertEquals(ResultadoEvento.CONFLICTO, resultado.resultado());
        assertTrue(resultado.detalle().contains("bloqueado"));
    }

    @Test
    @Tag("RN-19")
    void noDeberiaDuplicarReservaAlRecibirElMismoMensajeDosVeces() {
        Reserva yaRecibida = externa("RES-2026-00010", BOOKING, ID_EXTERNO);

        IntegracionExterna resultado = integrar(booking(true), Optional.of(yaRecibida),
                disponibilidadCon(List.of(yaRecibida)), () -> {
                    throw new AssertionError("No debe crear otra reserva");
                });

        assertEquals(ResultadoEvento.DUPLICADO, resultado.resultado());
        assertSame(yaRecibida, resultado.reserva());
        assertTrue(resultado.reservaCreada().isEmpty());
    }

    @Test
    @Tag("RN-19")
    void deberiaCrearReservaExternaConIdentificadorNuevo() {
        Reserva otraDeBooking = externa("RES-2026-00010", BOOKING, "BK-01");

        IntegracionExterna resultado = integrar(booking(true), Optional.empty(), disponibilidadCon(List.of()),
                () -> externa("RES-2026-00011", BOOKING, ID_EXTERNO));

        assertEquals(ResultadoEvento.EXITOSO, resultado.resultado());
        assertFalse(resultado.reserva().equals(otraDeBooking));
    }

    @Test
    @Tag("CAN-05")
    void unCanalInactivoNoCreaReservas() {
        assertThrows(ReglaDominioException.class, () -> integrar(booking(false), Optional.empty(),
                disponibilidadCon(List.of()), () -> externa("RES-2026-00010", BOOKING, ID_EXTERNO)));
    }

    @Test
    @Tag("CORI-03")
    void deberiaRechazarUnaReservaQueNoCorrespondeAlCanal() {
        assertThrows(ReglaDominioException.class, () -> integrar(booking(true), Optional.empty(),
                disponibilidadCon(List.of()), () -> externa("RES-2026-00010", new CanalId("CAN-9"), ID_EXTERNO)));
    }
}
