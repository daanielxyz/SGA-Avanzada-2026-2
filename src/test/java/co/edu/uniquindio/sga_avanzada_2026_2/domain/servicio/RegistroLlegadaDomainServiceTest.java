package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoEn;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistroLlegadaDomainServiceTest {

    private static final RegistroId REGISTRO = new RegistroId("REG-1");
    private static final UsuarioId RECEPCION = new UsuarioId("USR-1");
    private static final LocalDateTime LLEGADA = dia(10).atTime(15, 30);

    private final RegistroLlegadaDomainService servicio = new RegistroLlegadaDomainService();

    @Test
    @Tag("RN-11")
    @Tag("REG-04")
    void deberiaPermitirCheckInSiApartamentoEstaPreparado() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Apartamento apartamento = apartamentoEn(EstadoOperativo.PREPARADO);

        servicio.procesarCheckIn(reserva, apartamento, REGISTRO, LLEGADA, RECEPCION);

        assertEquals(EstadoReserva.EN_CURSO, reserva.estado());
        assertEquals(EstadoOperativo.OCUPADO, apartamento.estadoOperativo());
        assertEquals(LLEGADA, reserva.registro().fechaHora());
        assertEquals(RECEPCION, reserva.registro().autor());
    }

    @Test
    @Tag("RN-11")
    @Tag("REG-04")
    void impedirCheckInSiApartamentoNoEstaPreparado() {
        Reserva reserva = confirmada("RES-2026-00001", 10, 12);
        Apartamento apartamento = apartamentoEn(EstadoOperativo.EN_PREPARACION);

        assertThrows(ReglaDominioException.class,
                () -> servicio.procesarCheckIn(reserva, apartamento, REGISTRO, LLEGADA, RECEPCION));
        assertEquals(EstadoReserva.CONFIRMADA, reserva.estado());
        assertEquals(EstadoOperativo.EN_PREPARACION, apartamento.estadoOperativo());
    }

    @Test
    @Tag("RN-10")
    @Tag("REG-04")
    void siLaReservaNoAdmiteElRegistroElApartamentoNoCambia() {
        Reserva pendiente = reserva("RES-2026-00001", ReservasDePrueba.APT, 10, 12, EstadoReserva.PENDIENTE);
        Apartamento apartamento = apartamentoEn(EstadoOperativo.PREPARADO);

        assertThrows(ReglaDominioException.class,
                () -> servicio.procesarCheckIn(pendiente, apartamento, REGISTRO, LLEGADA, RECEPCION));
        assertThrows(ReglaDominioException.class, () -> servicio.procesarCheckIn(confirmada("RES-2026-00002", 10, 12),
                apartamento, REGISTRO, dia(9).atTime(20, 0), RECEPCION));
        assertEquals(EstadoOperativo.PREPARADO, apartamento.estadoOperativo());
    }

    @Test
    void deberiaRechazarElApartamentoDeOtraReserva() {
        Reserva deOtroApartamento = reserva("RES-2026-00001", new ApartamentoId("APT-102"), 10, 12,
                EstadoReserva.CONFIRMADA);

        assertThrows(ReglaDominioException.class, () -> servicio.procesarCheckIn(deOtroApartamento,
                apartamentoEn(EstadoOperativo.PREPARADO), REGISTRO, LLEGADA, RECEPCION));
    }
}
