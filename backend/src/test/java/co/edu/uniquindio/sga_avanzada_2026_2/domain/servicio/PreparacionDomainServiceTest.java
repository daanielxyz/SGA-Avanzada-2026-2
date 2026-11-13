package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

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
import java.time.LocalTime;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hoy es el 10 de diciembre a las 9:00; la hora límite de llegada es 22:00. Los apartamentos se pasan en orden de
 * código al revés, para que se note que el orden sale de las llegadas.
 */
class PreparacionDomainServiceTest {

    private static final LocalDateTime AHORA = dia(10).atTime(9, 0);
    private static final LocalTime HORA_LIMITE = LocalTime.of(22, 0);

    private final PreparacionDomainService servicio = new PreparacionDomainService();

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void deberiaPonerPrimeroElQueTieneLaLlegadaMasCercana() {
        List<Apartamento> pendientes = List.of(apto("APT-104"), apto("APT-103"), apto("APT-102"), apto("APT-101"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 20, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)),
                reserva("RES-2026-00002", "APT-102", 10, EstadoReserva.CONFIRMADA, LocalTime.of(13, 0)),
                reserva("RES-2026-00003", "APT-104", 11, EstadoReserva.PENDIENTE, null));

        assertEquals(List.of("APT-102", "APT-104", "APT-101", "APT-103"), codigos(pendientes, reservas, AHORA));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void deberiaTomarLaPrimeraDeVariasReservasDelMismoApartamento() {
        List<Apartamento> pendientes = List.of(apto("APT-102"), apto("APT-101"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 25, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)),
                reserva("RES-2026-00002", "APT-101", 12, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)),
                reserva("RES-2026-00003", "APT-102", 14, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)));

        assertEquals(List.of("APT-101", "APT-102"), codigos(pendientes, reservas, AHORA));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void elMismoDiaDeberiaIrPrimeroLaHoraMasTempranaYAlFinalLaQueNoTieneHora() {
        List<Apartamento> pendientes = List.of(apto("APT-101"), apto("APT-102"), apto("APT-103"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 10, EstadoReserva.PENDIENTE, null),
                reserva("RES-2026-00002", "APT-102", 10, EstadoReserva.CONFIRMADA, LocalTime.of(18, 0)),
                reserva("RES-2026-00003", "APT-103", 10, EstadoReserva.CONFIRMADA, LocalTime.of(12, 0)));

        assertEquals(List.of("APT-103", "APT-102", "APT-101"), codigos(pendientes, reservas, AHORA));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void sinLlegadasDeberiaQuedarPorCodigo() {
        List<Apartamento> pendientes = List.of(apto("APT-103"), apto("APT-101"), apto("APT-102"));

        assertEquals(List.of("APT-101", "APT-102", "APT-103"), codigos(pendientes, List.of(), AHORA));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void noDeberiaAdelantarUnApartamentoPorReservasQueYaNoLlegan() {
        List<Apartamento> pendientes = List.of(apto("APT-104"), apto("APT-103"), apto("APT-102"), apto("APT-101"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 10, EstadoReserva.CANCELADA, LocalTime.of(13, 0)),
                reserva("RES-2026-00002", "APT-102", 9, EstadoReserva.CONFIRMADA, LocalTime.of(13, 0)),
                reserva("RES-2026-00003", "APT-103", 10, EstadoReserva.NO_SHOW, LocalTime.of(13, 0)),
                reserva("RES-2026-00004", "APT-104", 15, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)));

        // Solo APT-104 espera a alguien; los demás, por código al final
        assertEquals(List.of("APT-104", "APT-101", "APT-102", "APT-103"), codigos(pendientes, reservas, AHORA));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-110")
    @Tag("DEC-111")
    void unaPendienteQueYaNoSePuedeConfirmarNoDeberiaAdelantarSuApartamento() {
        List<Apartamento> pendientes = List.of(apto("APT-101"), apto("APT-102"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 10, EstadoReserva.PENDIENTE, LocalTime.of(15, 0)),
                reserva("RES-2026-00002", "APT-102", 12, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)));

        LocalDateTime pasadaLaHoraLimite = dia(10).atTime(22, 30);

        assertEquals(List.of("APT-102", "APT-101"), codigos(pendientes, reservas, pasadaLaHoraLimite));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-111")
    void unaConfirmadaDeHoyDeberiaSeguirEsperandosePasadaLaHoraLimite() {
        List<Apartamento> pendientes = List.of(apto("APT-102"), apto("APT-101"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 10, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)),
                reserva("RES-2026-00002", "APT-102", 12, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)));

        assertEquals(List.of("APT-101", "APT-102"), codigos(pendientes, reservas, dia(10).atTime(22, 30)));
    }

    @Test
    @Tag("CU-41")
    @Tag("DEC-110")
    @Tag("DEC-111")
    void unaPendienteCreadaDespuesDeLaHoraLimiteDeberiaSeguirEsperandose() {
        List<Apartamento> pendientes = List.of(apto("APT-102"), apto("APT-101"));
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", "APT-101", 10, EstadoReserva.PENDIENTE, LocalTime.of(23, 0),
                        dia(10).atTime(22, 15)),
                reserva("RES-2026-00002", "APT-102", 12, EstadoReserva.CONFIRMADA, LocalTime.of(15, 0)));

        assertEquals(List.of("APT-101", "APT-102"), codigos(pendientes, reservas, dia(10).atTime(22, 30)));
    }

    private List<String> codigos(List<Apartamento> pendientes, List<Reserva> reservas, LocalDateTime ahora) {
        return servicio.ordenarPorProximaLlegada(pendientes, reservas, ahora, HORA_LIMITE).stream()
                .map(a -> a.codigo().valor())
                .toList();
    }

    private static Apartamento apto(String codigo) {
        return new Apartamento(new ApartamentoId(codigo), new AlojamientoId("ALO-1"), "Apto " + codigo, null,
                new Capacidad(4), new Dormitorio(2), EstadoOperativo.PENDIENTE_PREPARACION,
                List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)), List.of(new Caracteristica("Balcón")),
                List.of(), true);
    }

    private static Reserva reserva(String codigo, String apartamento, int entrada, EstadoReserva estado,
                                   LocalTime hora) {
        return reserva(codigo, apartamento, entrada, estado, hora, dia(1).atStartOfDay());
    }

    private static Reserva reserva(String codigo, String apartamento, int entrada, EstadoReserva estado,
                                   LocalTime hora, LocalDateTime creadaEn) {
        Estancia estancia = new Estancia(dia(entrada), dia(entrada + 2));
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-BASE"), "Temporada base", Dinero.de(100_000), 1,
                        Dinero.de(100_000)))
                .toList();
        return new Reserva(new ReservaId(codigo), new ApartamentoId(apartamento), new TitularId("TIT-1"), estancia,
                estado, CanalOrigen.DIRECTO, null, null,
                List.of(new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 1, 1), null, null)),
                List.of(), null, hora, Dinero.de(100_000L * desglose.size()), desglose, new PoliticaId("POL-1"),
                creadaEn);
    }
}
