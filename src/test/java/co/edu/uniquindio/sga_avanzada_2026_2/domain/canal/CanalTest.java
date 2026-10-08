package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanalTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 9, 0);

    private static Canal externo() {
        return new Canal(new CanalId("CAN-1"), ALO, "Booking", CanalOrigen.EXTERNO, "sga.canales.booking", true);
    }

    private static ConflictoCanal conflicto() {
        return ConflictoCanal.registrar(new ConflictoId("CON-1"), new CanalId("CAN-1"), "BK-77",
                new ApartamentoId("APT-101"), new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                AHORA, "Ya está reservado en esas fechas");
    }

    @Test
    @Tag("CAN-01")
    @Tag("CAN-02")
    void soloUnCanalExternoTieneReferenciaACredencial() {
        assertDoesNotThrow(CanalTest::externo);
        assertDoesNotThrow(() -> new Canal(new CanalId("CAN-2"), ALO, "Portal", CanalOrigen.PORTAL, null, true));
        assertThrows(ReglaDominioException.class,
                () -> new Canal(new CanalId("CAN-1"), ALO, "Booking", CanalOrigen.EXTERNO, " ", true));
        assertThrows(ReglaDominioException.class,
                () -> new Canal(new CanalId("CAN-2"), ALO, "Portal", CanalOrigen.PORTAL, "sga.canales.portal", true));
    }

    @Test
    @Tag("CAN-05")
    void unCanalDesactivadoNoTraeReservasNuevas() {
        Canal canal = externo();
        assertDoesNotThrow(canal::exigirReservasExternas);

        canal.desactivar();

        assertFalse(canal.activo());
        assertThrows(ReglaDominioException.class, canal::exigirReservasExternas);
        assertThrows(ReglaDominioException.class, canal::desactivar);
    }

    @Test
    @Tag("CAN-05")
    void unCanalNoExternoNoTraeReservasExternas() {
        Canal directo = new Canal(new CanalId("CAN-2"), ALO, "Recepción", CanalOrigen.DIRECTO, null, true);

        assertThrows(ReglaDominioException.class, directo::exigirReservasExternas);
    }

    @Test
    @Tag("CONF-01")
    @Tag("CONF-03")
    void unConflictoNacePendienteConElMotivo() {
        ConflictoCanal conflicto = conflicto();

        assertEquals(EstadoConflicto.PENDIENTE, conflicto.estado());
        assertEquals("Ya está reservado en esas fechas", conflicto.motivo());
        assertEquals(AHORA, conflicto.detectadoEn());
        assertNull(conflicto.decision());
    }

    @Test
    @Tag("CONF-04")
    void soloSeResuelveUnaVezConAutorFechaYDecision() {
        ConflictoCanal conflicto = conflicto();
        UsuarioId admin = new UsuarioId("ADM-1");

        assertThrows(ReglaDominioException.class, () -> conflicto.resolver(admin, " ", AHORA));
        conflicto.resolver(admin, "Se contactó al huésped y se reubicó", AHORA.plusHours(2));

        assertEquals(EstadoConflicto.RESUELTO, conflicto.estado());
        assertEquals(admin, conflicto.resueltoPor());
        assertEquals(AHORA.plusHours(2), conflicto.resueltoEn());
        assertThrows(ReglaDominioException.class, () -> conflicto.resolver(admin, "Otra", AHORA.plusHours(3)));
    }

    @Test
    @Tag("BIT-01")
    void unEventoRegistraTodosSusDatos() {
        assertDoesNotThrow(() -> new EventoCanal(new EventoCanalId("EVT-1"), new CanalId("CAN-1"),
                OperacionCanal.CREAR_RESERVA, SentidoEvento.ENTRANTE, AHORA, "{\"id\":\"BK-77\"}",
                ResultadoEvento.CONFLICTO, "Ya está reservado"));
        assertThrows(ReglaDominioException.class, () -> new EventoCanal(new EventoCanalId("EVT-1"),
                new CanalId("CAN-1"), OperacionCanal.CREAR_RESERVA, SentidoEvento.ENTRANTE, AHORA, "{}", null, null));
    }
}
