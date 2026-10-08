package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EstadoConflicto;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway de los tres agregados de canales: Canal, ConflictoCanal y la
 * bitácora EventoCanal.
 */
@DataJpaTest
@Import({CanalRepositoryJpa.class, ConflictoCanalRepositoryJpa.class, EventoCanalRepositoryJpa.class})
class CanalesRepositoryJpaTest {

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");

    private static final CanalId BOOKING = new CanalId("CAN-1");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Autowired
    private CanalRepositoryJpa canales;

    @Autowired
    private ConflictoCanalRepositoryJpa conflictos;

    @Autowired
    private EventoCanalRepositoryJpa eventos;

    @Autowired
    private EntityManager entityManager;

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static ConflictoCanal conflicto(String id) {
        return ConflictoCanal.registrar(new ConflictoId(id), BOOKING, "BK-77", new ApartamentoId("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), AHORA, "Ya está reservado");
    }

    private static EventoCanal evento(String id, LocalDateTime cuando) {
        return new EventoCanal(new EventoCanalId(id), BOOKING, OperacionCanal.CREAR_RESERVA, SentidoEvento.ENTRANTE,
                cuando, "{\"id\":\"BK-77\"}", ResultadoEvento.CONFLICTO, "Ya está reservado");
    }

    @Test
    @Tag("CAN-05")
    void deberiaGuardarElCanalYSuDesactivacion() {
        canales.guardar(new Canal(BOOKING, ALO, "Booking", CanalOrigen.EXTERNO, "sga.canales.booking", true));
        sincronizar();

        Canal canal = canales.buscarPorId(BOOKING).orElseThrow();
        assertEquals("sga.canales.booking", canal.referenciaCredencial());
        canal.desactivar();
        canales.guardar(canal);
        sincronizar();

        assertFalse(canales.buscarPorId(BOOKING).orElseThrow().activo());
    }

    @Test
    @Tag("CONF-04")
    void deberiaGuardarElConflictoYSuResolucion() {
        conflictos.guardar(conflicto("CON-1"));
        conflictos.guardar(conflicto("CON-2"));
        sincronizar();

        ConflictoCanal conflicto = conflictos.buscarPorId(new ConflictoId("CON-1")).orElseThrow();
        assertEquals(new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                conflicto.estanciaSolicitada());
        assertNull(conflicto.resueltoPor());
        conflicto.resolver(new UsuarioId("ADM-1"), "Reubicado en APT-102", AHORA.plusHours(1));
        conflictos.guardar(conflicto);
        sincronizar();

        ConflictoCanal resuelto = conflictos.buscarPorId(new ConflictoId("CON-1")).orElseThrow();
        assertEquals(EstadoConflicto.RESUELTO, resuelto.estado());
        assertEquals("Reubicado en APT-102", resuelto.decision());
        assertEquals(List.of(new ConflictoId("CON-2")),
                conflictos.buscarPendientes().stream().map(ConflictoCanal::id).toList());
    }

    @Test
    @Tag("BIT-01")
    @Tag("BIT-02")
    void laBitacoraSoloAgregaYSeConsultaPorRango() {
        eventos.registrar(evento("EVT-1", AHORA));
        eventos.registrar(evento("EVT-2", AHORA.plusHours(2)));
        eventos.registrar(evento("EVT-3", AHORA.plusDays(3)));
        sincronizar();

        List<EventoCanal> delDia = eventos.buscarPorCanal(BOOKING, AHORA, AHORA.plusDays(1));

        assertEquals(List.of(new EventoCanalId("EVT-2"), new EventoCanalId("EVT-1")),
                delDia.stream().map(EventoCanal::id).toList());
        assertEquals(ResultadoEvento.CONFLICTO, delDia.getFirst().resultado());
        assertThrows(ReglaDominioException.class, () -> eventos.registrar(evento("EVT-1", AHORA)));
    }
}
