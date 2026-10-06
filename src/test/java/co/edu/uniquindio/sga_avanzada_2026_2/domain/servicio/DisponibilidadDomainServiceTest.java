package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.BloqueoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.APT;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamento;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.apartamentoActivo;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.dia;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.parametros;
import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibilidadDomainServiceTest {

    private static final Estancia DEL_10_AL_12 = new Estancia(dia(10), dia(12));

    private final DisponibilidadDomainService servicio = new DisponibilidadDomainService();

    private Disponibilidad verificar(List<Reserva> reservas, int horasPreparacion) {
        return servicio.verificarDisponibilidad(apartamentoActivo(), DEL_10_AL_12, 2, reservas,
                parametros(horasPreparacion));
    }

    @Test
    @Tag("RN-01")
    void deberiaPermitirReservaEnRangoSinSolapamiento() {
        Disponibilidad disponibilidad = verificar(List.of(confirmada("RES-2026-00001", 5, 8)), 3);

        assertTrue(disponibilidad.disponible());
        assertNull(disponibilidad.motivo());
        assertDoesNotThrow(disponibilidad::exigir);
    }

    @Test
    @Tag("RN-01")
    void deberiaLanzarExcepcionAlDetectarSolapamiento() {
        Disponibilidad disponibilidad = verificar(List.of(confirmada("RES-2026-00001", 11, 14)), 3);

        assertFalse(disponibilidad.disponible());
        ReglaDominioException error = assertThrows(ReglaDominioException.class, disponibilidad::exigir);
        assertTrue(error.getMessage().contains("RES-2026-00001"));
    }

    @Test
    @Tag("RN-12")
    @Tag("CORI-02")
    void deberiaIgnorarReservasInactivasYDeOtrosApartamentos() {
        List<Reserva> reservas = List.of(
                reserva("RES-2026-00001", APT, 10, 12, EstadoReserva.CANCELADA),
                reserva("RES-2026-00002", APT, 10, 12, EstadoReserva.NO_SHOW),
                reserva("RES-2026-00003", new ApartamentoId("APT-102"), 10, 12, EstadoReserva.CONFIRMADA));

        assertTrue(verificar(reservas, 3).disponible());
    }

    @Test
    @Tag("RN-07")
    void deberiaPermitirReservaEnRangoSinBloqueoActivo() {
        Bloqueo levantado = new Bloqueo(new BloqueoId("BLO-1"), dia(10), dia(11), "Pintura", false);
        Bloqueo posterior = new Bloqueo(new BloqueoId("BLO-2"), dia(12), dia(15), "Pintura", true);

        assertTrue(servicio.verificarDisponibilidad(apartamento(true, List.of(levantado, posterior)), DEL_10_AL_12,
                2, List.of(), parametros(3)).disponible());
    }

    @Test
    @Tag("RN-07")
    void deberiaImpedirReservaEnRangoDeFechasConBloqueoActivo() {
        Bloqueo bloqueo = new Bloqueo(new BloqueoId("BLO-1"), dia(11), dia(13), "Plomería", true);

        Disponibilidad disponibilidad = servicio.verificarDisponibilidad(apartamento(true, List.of(bloqueo)),
                DEL_10_AL_12, 2, List.of(), parametros(3));

        assertFalse(disponibilidad.disponible());
        assertTrue(disponibilidad.motivo().contains("bloqueado"));
    }

    @Test
    @Tag("DISP-02")
    void deberiaImpedirReservarUnApartamentoInactivoOConGrupoQueNoCabe() {
        assertFalse(servicio.verificarDisponibilidad(apartamento(false, List.of()), DEL_10_AL_12, 2, List.of(),
                parametros(3)).disponible());
        assertFalse(servicio.verificarDisponibilidad(apartamentoActivo(), DEL_10_AL_12, 5, List.of(),
                parametros(3)).disponible());
    }

    @Test
    @Tag("RN-20")
    void deberiaPermitirReservaSiSeRespetaElTiempoDePreparacion() {
        List<Reserva> contiguas = List.of(confirmada("RES-2026-00001", 8, 10), confirmada("RES-2026-00002", 12, 14));

        assertTrue(verificar(contiguas, 4).disponible()); // 4 h caben en la ventana 11:00 → 15:00
    }

    @Test
    @Tag("RN-20")
    void deberiaRechazarReservaSiNoRespetaTiempoPreparacion() {
        Disponibilidad trasUnaSalida = verificar(List.of(confirmada("RES-2026-00001", 8, 10)), 5);
        Disponibilidad antesDeUnaEntrada = verificar(List.of(confirmada("RES-2026-00002", 12, 14)), 5);

        assertFalse(trasUnaSalida.disponible());
        assertFalse(antesDeUnaEntrada.disponible());
        assertTrue(verificar(List.of(confirmada("RES-2026-00003", 7, 9)), 5).disponible());
    }

    @Test
    @Tag("RN-14")
    void alModificarNoDeberiaChocarConSusPropiasNoches() {
        Reserva propia = confirmada("RES-2026-00001", 10, 12);
        Estancia unaNocheMas = new Estancia(dia(10), dia(13));

        Disponibilidad disponibilidad = servicio.verificarParaModificar(propia, apartamentoActivo(), unaNocheMas, 2,
                List.of(propia), parametros(3));

        assertTrue(disponibilidad.disponible());
        assertEquals(unaNocheMas, disponibilidad.estancia());
    }
}
