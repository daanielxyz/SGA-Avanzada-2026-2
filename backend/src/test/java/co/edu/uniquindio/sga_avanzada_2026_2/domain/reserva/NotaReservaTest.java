package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MotivoRechazo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Rechazos.exigeMotivo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Notas de la reserva para el siguiente turno (BL-08 · DEC-105).
 */
class NotaReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 14, 20);
    private static final Estancia ESTANCIA = new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12));
    private static final UsuarioId RECEPCION = new UsuarioId("USR-2");
    private static final UsuarioId ADMIN = new UsuarioId("USR-4");
    private static final NotaReservaId NOTA = new NotaReservaId("NOT-1");

    private static Reserva pendiente() {
        Ocupante ana = new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 3, 1),
                new Documento(TipoDocumento.CC, "1094"), null);
        Titular titular = new Titular(new TitularId("TIT-1"), new AlojamientoId("ALO-1"), "Ana", ana.documento(),
                null, "3001234567");
        List<LineaCotizacion> lineas = ESTANCIA.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-BASE"), Dinero.de(100_000), 1,
                        Dinero.de(100_000)))
                .toList();
        return Reserva.crear(new ReservaId("RES-2026-00001"), new ApartamentoId("APT-101"), titular, ESTANCIA,
                CanalOrigen.DIRECTO, null, null, List.of(ana), LocalTime.of(15, 0),
                new Cotizacion(lineas, Dinero.de(200_000)), new PoliticaId("POL-1"), new Capacidad(4), 0,
                AHORA.minusDays(1));
    }

    private static Reserva cancelada() {
        Reserva reserva = pendiente();
        reserva.cancelar();
        return reserva;
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void deberiaEscribirUnaNotaPendienteConSuAutorYFecha() {
        NotaReserva nota = NotaReserva.escribir(NOTA, pendiente(), "  Pagaron la mitad, cobrar el resto ",
                RECEPCION, AHORA);

        assertEquals("Pagaron la mitad, cobrar el resto", nota.texto());
        assertEquals(new ReservaId("RES-2026-00001"), nota.reservaId());
        assertEquals(RECEPCION, nota.autor());
        assertEquals(AHORA, nota.escritaEn());
        assertFalse(nota.atendida());
        assertNull(nota.atencion());
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void noDeberiaEscribirUnaNotaEnUnaReservaTerminada() {
        exigeMotivo(MotivoRechazo.ESTADO_NO_PERMITIDO,
                () -> NotaReserva.escribir(NOTA, cancelada(), "Llamó a reclamar", RECEPCION, AHORA));
    }

    @Test
    @Tag("BL-08")
    void noDeberiaEscribirUnaNotaVaciaNiMuyLarga() {
        assertThrows(ReglaDominioException.class,
                () -> NotaReserva.escribir(NOTA, pendiente(), "   ", RECEPCION, AHORA));
        assertThrows(ReglaDominioException.class,
                () -> NotaReserva.escribir(NOTA, pendiente(), "x".repeat(NotaReserva.LARGO_MAXIMO + 1),
                        RECEPCION, AHORA));
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void deberiaAtenderUnaNotaDejandoQuienYCuando() {
        Reserva reserva = pendiente();
        NotaReserva nota = NotaReserva.escribir(NOTA, reserva, "Falta la cédula", RECEPCION, AHORA);

        nota.atender(reserva, ADMIN, AHORA.plusHours(3));

        assertTrue(nota.atendida());
        assertEquals(new AtencionNota(ADMIN, AHORA.plusHours(3)), nota.atencion());
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void noDeberiaAtenderDosVecesLaMismaNota() {
        Reserva reserva = pendiente();
        NotaReserva nota = NotaReserva.escribir(NOTA, reserva, "Falta la cédula", RECEPCION, AHORA);
        nota.atender(reserva, ADMIN, AHORA.plusHours(1));

        exigeMotivo(MotivoRechazo.YA_ATENDIDA, () -> nota.atender(reserva, RECEPCION, AHORA.plusHours(2)));
        assertEquals(ADMIN, nota.atencion().autor());
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void noDeberiaAtenderUnaNotaSiLaReservaYaTermino() {
        Reserva reserva = pendiente();
        NotaReserva nota = NotaReserva.escribir(NOTA, reserva, "Pidieron cobijas", RECEPCION, AHORA);
        reserva.cancelar();

        exigeMotivo(MotivoRechazo.ESTADO_NO_PERMITIDO, () -> nota.atender(reserva, ADMIN, AHORA.plusHours(1)));
        assertFalse(nota.atendida());
    }

    @Test
    @Tag("BL-08")
    void noDeberiaAtenderseAntesDeEscribirse() {
        Reserva reserva = pendiente();
        NotaReserva nota = NotaReserva.escribir(NOTA, reserva, "Sale temprano", RECEPCION, AHORA);

        assertThrows(ReglaDominioException.class, () -> nota.atender(reserva, ADMIN, AHORA.minusMinutes(1)));
        assertThrows(ReglaDominioException.class, () -> new NotaReserva(NOTA, reserva.codigo(), "Sale temprano",
                RECEPCION, AHORA, new AtencionNota(ADMIN, AHORA.minusMinutes(1))));
    }
}
