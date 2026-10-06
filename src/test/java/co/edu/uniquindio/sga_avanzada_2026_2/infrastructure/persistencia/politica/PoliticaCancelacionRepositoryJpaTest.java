package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(PoliticaCancelacionRepositoryJpa.class)
class PoliticaCancelacionRepositoryJpaTest {

    private static final AlojamientoId ALOJAMIENTO = new AlojamientoId("ALO-1");
    private static final Penalizacion TODO = Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(100));
    private static final Penalizacion FIJO = Penalizacion.montoFijo(BaseRetencion.PAGADO, Dinero.de(80_000));
    private static final Penalizacion NO_SHOW =
            Penalizacion.porcentaje(BaseRetencion.ANTICIPO_EXIGIDO, new Porcentaje(100));

    @Autowired
    private PoliticaCancelacionRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private static PoliticaCancelacion v1() {
        return PoliticaCancelacion.crear(new PoliticaId("POL-1"), ALOJAMIENTO,
                List.of(new TramoCancelacion(0, TODO), new TramoCancelacion(48, FIJO)), NO_SHOW, 2);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deberiaCargarLaPoliticaCompletaTalComoSeGuardo() {
        repositorio.guardar(v1());
        sincronizar();

        PoliticaCancelacion cargada = repositorio.buscarPorId(new PoliticaId("POL-1")).orElseThrow();

        assertEquals(ALOJAMIENTO, cargada.alojamientoId());
        assertEquals(1, cargada.version());
        assertEquals(v1().tramos(), cargada.tramos());
        assertEquals(NO_SHOW, cargada.penalizacionNoShow());
        assertEquals(FIJO, cargada.penalizacionPara(50));
    }

    @Test
    @Tag("POL-01")
    void laVigenteEsLaVersionMasAltaDelAlojamiento() {
        PoliticaCancelacion v1 = v1();
        repositorio.guardar(v1);
        repositorio.guardar(v1.nuevaVersion(new PoliticaId("POL-2"), List.of(new TramoCancelacion(0, FIJO)),
                TODO, 1));
        sincronizar();

        assertEquals(new PoliticaId("POL-2"), repositorio.buscarVigente(ALOJAMIENTO).orElseThrow().id());
        assertEquals(1, repositorio.buscarPorId(new PoliticaId("POL-1")).orElseThrow().version());
        assertTrue(repositorio.buscarVigente(new AlojamientoId("ALO-9")).isEmpty());
    }

    @Test
    @Tag("POL-04")
    void unaVersionGuardadaNoSeSobrescribe() {
        repositorio.guardar(v1());
        sincronizar();

        assertThrows(ReglaDominioException.class, () -> repositorio.guardar(v1()));
    }
}
