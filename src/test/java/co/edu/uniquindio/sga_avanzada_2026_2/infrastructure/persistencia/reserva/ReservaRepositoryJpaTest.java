package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Registro;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Salida;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(ReservaRepositoryJpa.class)
class ReservaRepositoryJpaTest {

    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 12, 1, 9, 30);
    private static final UsuarioId RECEPCION = new UsuarioId("USR-1");
    private static final Ocupante ANA = new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 3, 1),
            new Documento(TipoDocumento.CC, "1094"), "Colombiana");
    private static final Ocupante NINO = new Ocupante(new OcupanteId("OCU-2"), "Tomás", LocalDate.of(2020, 6, 1),
            null, null);

    @Autowired
    private ReservaRepositoryJpa repositorio;

    @Autowired
    private ReservaJpaRepository filas;

    @Autowired
    private EntityManager entityManager;

    private static Reserva reserva(String codigo, ApartamentoId apartamento, int entrada, int salida,
                                   EstadoReserva estado, CanalOrigen canal, CanalId canalId, String idExterno,
                                   Registro registro, Salida salidaReal) {
        Estancia estancia = new Estancia(LocalDate.of(2026, 12, entrada), LocalDate.of(2026, 12, salida));
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-ALTA"), Dinero.de(150_000), 1,
                        Dinero.de(150_000)))
                .toList();
        return new Reserva(new ReservaId(codigo), apartamento, new TitularId("TIT-1"), estancia, estado, canal,
                canalId, idExterno, List.of(ANA, NINO), registro, salidaReal, LocalTime.of(15, 30),
                Dinero.de(150_000L * desglose.size()), desglose, new PoliticaId("POL-1"), CREADA);
    }

    private static Reserva pendiente(String codigo, int entrada, int salida) {
        return reserva(codigo, APT, entrada, salida, EstadoReserva.PENDIENTE, CanalOrigen.PORTAL, null, null, null,
                null);
    }

    private static Reserva externa(String codigo, String idExterno) {
        return reserva(codigo, APT, 20, 22, EstadoReserva.PENDIENTE, CanalOrigen.EXTERNO, new CanalId("CAN-1"),
                idExterno, null, null);
    }

    /** Obliga a escribir en la base y a releer desde ella, no desde la caché de JPA. */
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarElAgregadoCompletoTalComoSeGuardo() {
        Registro registro = new Registro(new RegistroId("REG-1"), LocalDateTime.of(2026, 12, 10, 15, 40), RECEPCION,
                false);
        Salida salida = new Salida(new SalidaId("SAL-1"), LocalDateTime.of(2026, 12, 13, 10, 5), RECEPCION);
        repositorio.guardar(reserva("RES-2026-00001", APT, 10, 13, EstadoReserva.FINALIZADA, CanalOrigen.EXTERNO,
                new CanalId("CAN-1"), "BK-77", registro, salida));
        sincronizar();

        Reserva cargada = repositorio.buscarPorCodigo(new ReservaId("RES-2026-00001")).orElseThrow();

        assertEquals(APT, cargada.apartamentoId());
        assertEquals(new TitularId("TIT-1"), cargada.titularId());
        assertEquals(new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13)), cargada.estancia());
        assertEquals(EstadoReserva.FINALIZADA, cargada.estado());
        assertEquals(CanalOrigen.EXTERNO, cargada.canalOrigen());
        assertEquals(new CanalId("CAN-1"), cargada.canalId());
        assertEquals("BK-77", cargada.idExterno());
        assertEquals(LocalTime.of(15, 30), cargada.horaEstimadaLlegada());
        assertEquals(Dinero.de(450_000), cargada.valorTotal());
        assertEquals(3, cargada.desglose().size());
        assertEquals(new LineaCotizacion(new Noche(LocalDate.of(2026, 12, 10)), new TemporadaId("TEM-ALTA"),
                Dinero.de(150_000), 1, Dinero.de(150_000)), cargada.desglose().getFirst());
        assertEquals(new PoliticaId("POL-1"), cargada.politicaVersionId());
        assertEquals(CREADA, cargada.creadaEn());
        assertEquals(registro.fechaHora(), cargada.registro().fechaHora());
        assertEquals(RECEPCION, cargada.registro().autor());
        assertFalse(cargada.registro().anulado());
        assertEquals(salida.fechaHora(), cargada.salida().fechaHora());
        Ocupante ana = cargada.ocupantes().getFirst();
        assertEquals(ANA.documento(), ana.documento());
        assertEquals("Colombiana", ana.nacionalidad());
        assertEquals(LocalDate.of(1990, 3, 1), ana.fechaNacimiento());
        assertNull(cargada.ocupantes().get(1).documento());
    }

    @Test
    void deberiaPersistirLosCambiosHechosPorElDominio() {
        repositorio.guardar(pendiente("RES-2026-00001", 10, 12));
        sincronizar();

        Reserva reserva = repositorio.buscarPorCodigo(new ReservaId("RES-2026-00001")).orElseThrow();
        reserva.confirmar(new Porcentaje(0), Dinero.CERO);
        reserva.registrarLlegada(new RegistroId("REG-1"), LocalDateTime.of(2026, 12, 10, 16, 0), RECEPCION);
        repositorio.guardar(reserva);
        sincronizar();

        Reserva cargada = repositorio.buscarPorCodigo(new ReservaId("RES-2026-00001")).orElseThrow();
        assertEquals(EstadoReserva.EN_CURSO, cargada.estado());
        assertEquals(new RegistroId("REG-1"), cargada.registro().id());
        assertNull(cargada.salida());
    }

    @Test
    void deberiaIncrementarLaVersionEnCadaActualizacion() {
        repositorio.guardar(pendiente("RES-2026-00001", 10, 12));
        sincronizar();
        long versionInicial = filas.findById("RES-2026-00001").orElseThrow().getVersion();
        sincronizar();

        Reserva reserva = repositorio.buscarPorCodigo(new ReservaId("RES-2026-00001")).orElseThrow();
        reserva.cancelar();
        repositorio.guardar(reserva);
        sincronizar();

        assertEquals(versionInicial + 1, filas.findById("RES-2026-00001").orElseThrow().getVersion());
    }

    @Test
    @Tag("EDO-03")
    void deberiaBuscarSoloLasReservasActivasDelApartamento() {
        repositorio.guardar(pendiente("RES-2026-00001", 10, 12));
        repositorio.guardar(reserva("RES-2026-00002", APT, 12, 14, EstadoReserva.CANCELADA, CanalOrigen.DIRECTO,
                null, null, null, null));
        repositorio.guardar(reserva("RES-2026-00003", new ApartamentoId("APT-102"), 10, 12, EstadoReserva.PENDIENTE,
                CanalOrigen.DIRECTO, null, null, null, null));
        sincronizar();

        List<Reserva> activas = repositorio.buscarActivasPorApartamento(APT);

        assertEquals(List.of(new ReservaId("RES-2026-00001")), activas.stream().map(Reserva::codigo).toList());
    }

    @Test
    @Tag("RN-19")
    void noDeberiaGuardarDosReservasConElMismoCanalEIdentificadorExterno() {
        repositorio.guardar(externa("RES-2026-00001", "BK-77"));
        sincronizar();

        repositorio.guardar(externa("RES-2026-00002", "BK-77"));

        assertThrows(PersistenceException.class, this::sincronizar);
    }

    @Test
    @Tag("RN-19")
    void deberiaAdmitirVariasReservasSinCanalExterno() {
        repositorio.guardar(pendiente("RES-2026-00001", 1, 3));
        repositorio.guardar(pendiente("RES-2026-00002", 5, 7));
        sincronizar();

        assertTrue(repositorio.buscarPorCodigo(new ReservaId("RES-2026-00002")).isPresent());
    }

    @Test
    void deberiaDevolverVacioSiNoExiste() {
        assertTrue(repositorio.buscarPorCodigo(new ReservaId("RES-2026-99999")).isEmpty());
    }
}
