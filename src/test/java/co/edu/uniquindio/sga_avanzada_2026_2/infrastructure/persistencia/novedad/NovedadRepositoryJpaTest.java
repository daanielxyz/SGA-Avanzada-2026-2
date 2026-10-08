package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.EstadoNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.GravedadNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.Novedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga, con su historial.
 */
@DataJpaTest
@Import(NovedadRepositoryJpa.class)
class NovedadRepositoryJpaTest {

    private static final ApartamentoId APT = new ApartamentoId("APT-101");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final UsuarioId SERVICIO = new UsuarioId("USR-3");

    @Autowired
    private NovedadRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Novedad novedad(String id, LocalDateTime cuando) {
        return Novedad.registrar(new NovedadId(id), APT, "Grifo gotea", GravedadNovedad.MEDIA, SERVICIO, cuando);
    }

    @Test
    @Tag("NOV-04")
    void deberiaGuardarElHistorialDePasos() {
        repositorio.guardar(novedad("NOV-1", AHORA));
        sincronizar();

        Novedad novedad = repositorio.buscarPorId(new NovedadId("NOV-1")).orElseThrow();
        novedad.avanzar(new UsuarioId("ADM-1"), AHORA.plusHours(1));
        repositorio.guardar(novedad);
        sincronizar();

        Novedad cargada = repositorio.buscarPorId(new NovedadId("NOV-1")).orElseThrow();
        assertEquals(EstadoNovedad.EN_REVISION, cargada.estado());
        assertEquals(2, cargada.historial().size());
        assertEquals(SERVICIO, cargada.autor());
        assertEquals(AHORA, cargada.registradaEn());
    }

    @Test
    @Tag("NOV-06")
    void deberiaListarElHistorialDelApartamentoDeLaMasRecienteALaMasAntigua() {
        repositorio.guardar(novedad("NOV-1", AHORA));
        repositorio.guardar(novedad("NOV-2", AHORA.plusDays(2)));
        repositorio.guardar(Novedad.registrar(new NovedadId("NOV-3"), new ApartamentoId("APT-102"), "Otra",
                GravedadNovedad.BAJA, SERVICIO, AHORA));
        sincronizar();

        assertEquals(List.of(new NovedadId("NOV-2"), new NovedadId("NOV-1")),
                repositorio.buscarPorApartamento(APT).stream().map(Novedad::id).toList());
    }
}
