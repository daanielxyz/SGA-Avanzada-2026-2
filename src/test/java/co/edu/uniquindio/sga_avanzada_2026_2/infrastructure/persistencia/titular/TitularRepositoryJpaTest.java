package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ida y vuelta contra H2 con el esquema de Flyway: lo que se guarda es lo que se carga.
 */
@DataJpaTest
@Import(TitularRepositoryJpa.class)
class TitularRepositoryJpaTest {

    private static final AlojamientoId ALOJAMIENTO = new AlojamientoId("ALO-1");
    private static final Documento CC = new Documento(TipoDocumento.CC, "1094");

    @Autowired
    private TitularRepositoryJpa repositorio;

    @Autowired
    private EntityManager entityManager;

    private static Titular ana() {
        return new Titular(new TitularId("TIT-1"), ALOJAMIENTO, "Ana", CC, new Correo("ana@correo.co"), null);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @Tag("TIT-05")
    void deberiaEncontrarAlTitularPorIdYPorDocumento() {
        repositorio.guardar(ana());
        sincronizar();

        Titular porDocumento = repositorio.buscarPorDocumento(ALOJAMIENTO, CC).orElseThrow();

        assertEquals(new TitularId("TIT-1"), porDocumento.id());
        assertEquals(new Correo("ana@correo.co"), porDocumento.correo());
        assertNull(porDocumento.telefono());
        assertTrue(repositorio.buscarPorDocumento(ALOJAMIENTO, new Documento(TipoDocumento.CC, "999")).isEmpty());
    }

    @Test
    void deberiaPersistirLaActualizacionDeDatos() {
        repositorio.guardar(ana());
        sincronizar();

        Titular titular = repositorio.buscarPorId(new TitularId("TIT-1")).orElseThrow();
        titular.actualizarDatos("Ana María", null, "3001234567");
        repositorio.guardar(titular);
        sincronizar();

        Titular cargado = repositorio.buscarPorId(new TitularId("TIT-1")).orElseThrow();
        assertEquals("Ana María", cargado.nombre());
        assertNull(cargado.correo());
        assertEquals("3001234567", cargado.telefono());
    }

    @Test
    @Tag("TIT-05")
    void dosTitularesNoPuedenTenerElMismoDocumento() {
        repositorio.guardar(ana());
        sincronizar();

        repositorio.guardar(new Titular(new TitularId("TIT-2"), ALOJAMIENTO, "Otra", CC, null, "3001234567"));

        assertThrows(PersistenceException.class, this::sincronizar);
    }
}
