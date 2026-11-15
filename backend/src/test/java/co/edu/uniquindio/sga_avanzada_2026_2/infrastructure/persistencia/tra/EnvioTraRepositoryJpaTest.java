package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EstadoEnvioTra;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El envío del TRA guardado y leído con Flyway sobre H2 (DEC-130). */
@DataJpaTest
@Import(EnvioTraRepositoryJpa.class)
@Tag("DEC-130")
class EnvioTraRepositoryJpaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 17, 10, 0);

    @Autowired
    private EnvioTraRepositoryJpa envios;

    @Autowired
    private EntityManager entityManager;

    /** La tabla tiene llave foránea a la reserva: se crea una mínima por SQL. */
    private void reserva(String codigo) {
        entityManager.createNativeQuery("""
                insert into reserva (codigo, apartamento_codigo, titular_id, entrada, salida, estado, canal_origen,
                                     valor_total, politica_version_id, creada_en, version)
                values (?1, 'APT-101', 'TIT-1', DATE '2026-11-17', DATE '2026-11-20', 'EN_CURSO', 'DIRECTO',
                        540000, 'POL-1', TIMESTAMP '2026-11-01 09:00:00', 0)
                """).setParameter(1, codigo).executeUpdate();
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void guardaLoQueYaEntroYLoRecuperaIgual() {
        reserva("RES-2026-00042");
        EnvioTra envio = EnvioTra.pendiente(new ReservaId("RES-2026-00042"));
        envios.guardar(envio);
        sincronizar();

        EnvioTra leido = envios.buscarPorReserva(new ReservaId("RES-2026-00042")).orElseThrow();
        leido.iniciarIntento(AHORA);
        leido.registrarPrincipal("19");
        leido.registrarAcompanante(new OcupanteId("OCU-2"));
        leido.fallar("El Ministerio respondió 500");
        envios.guardar(leido);
        sincronizar();

        EnvioTra conError = envios.buscarPorReserva(new ReservaId("RES-2026-00042")).orElseThrow();
        assertEquals(EstadoEnvioTra.CON_ERROR, conError.estado());
        assertEquals("19", conError.codigoPrincipal());
        assertEquals(Set.of(new OcupanteId("OCU-2")), conError.acompanantesEnviados());
        assertEquals("El Ministerio respondió 500", conError.ultimoError());
        assertEquals(1, conError.intentos());
        assertEquals(AHORA, conError.ultimoIntento());
    }

    @Test
    void elPlanificadorVeLosPendientesYLosConErrorBajoElMaximo() {
        reserva("RES-2026-00001");
        reserva("RES-2026-00002");
        reserva("RES-2026-00003");
        envios.guardar(EnvioTra.pendiente(new ReservaId("RES-2026-00001")));
        EnvioTra fallido = EnvioTra.pendiente(new ReservaId("RES-2026-00002"));
        fallido.iniciarIntento(AHORA);
        fallido.fallar("Falta el RNT");
        envios.guardar(fallido);
        EnvioTra enviado = EnvioTra.pendiente(new ReservaId("RES-2026-00003"));
        enviado.iniciarIntento(AHORA);
        enviado.registrarPrincipal("7");
        enviado.terminar(AHORA);
        envios.guardar(enviado);
        sincronizar();

        assertEquals(List.of(new ReservaId("RES-2026-00001"), new ReservaId("RES-2026-00002")),
                envios.buscarParaEnviar(12));
        assertEquals(List.of(new ReservaId("RES-2026-00001")), envios.buscarParaEnviar(1));
        assertEquals(1, envios.listarPorEstado(EstadoEnvioTra.CON_ERROR, 0).totalElementos());
        assertTrue(envios.listarPorEstado(EstadoEnvioTra.ENVIADO, 0).contenido().getFirst().enviadoEn() != null);
    }
}
