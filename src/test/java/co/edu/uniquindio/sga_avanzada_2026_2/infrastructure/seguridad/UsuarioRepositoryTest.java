package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La tabla {@code usuario} de V11 contra H2 (Flyway + validate) y las reglas que cuida {@link Usuario}.
 */
@DataJpaTest
class UsuarioRepositoryTest {

    private static final String RESUMEN = "$2a$10$resumen-de-prueba";

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private EntityManager em;

    private static Usuario usuario(String id, String correo, Rol rol) {
        return Usuario.crear(new UsuarioId(id), "ALO-1", " Rita Gómez ", new Correo(correo), RESUMEN, rol);
    }

    @Test
    @Tag("USU-01")
    @Tag("CORR-01")
    void deberiaGuardarYBuscarPorElCorreoNormalizado() {
        usuarios.saveAndFlush(usuario("USR-901", " Rita@PuertaAlSol.CO ", Rol.RECEPCIONISTA));
        em.clear();

        Usuario leido = usuarios.findByCorreo("rita@puertaalsol.co").orElseThrow();

        assertEquals("USR-901", leido.id());
        assertEquals("Rita Gómez", leido.nombre());
        assertEquals(Rol.RECEPCIONISTA, leido.rol());
        assertEquals(RESUMEN, leido.contrasena());
        assertTrue(leido.puedeIniciarSesion());
        assertFalse(usuarios.existsByRolAndEliminadoFalse(Rol.ADMINISTRADOR));
    }

    @Test
    @Tag("USU-01")
    void elCorreoNoSeRepite() {
        usuarios.saveAndFlush(usuario("USR-901", "rita@puertaalsol.co", Rol.RECEPCIONISTA));

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarios.saveAndFlush(usuario("USR-902", "RITA@puertaalsol.co", Rol.ADMINISTRADOR)));
    }

    @Test
    @Tag("ROL-02")
    void unUsuarioNoNaceSinRolNombreNiContrasena() {
        Correo correo = new Correo("rita@puertaalsol.co");
        UsuarioId id = new UsuarioId("USR-901");

        assertThrows(IllegalArgumentException.class,
                () -> Usuario.crear(id, "ALO-1", "Rita", correo, RESUMEN, null));
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.crear(id, "ALO-1", " ", correo, RESUMEN, Rol.HUESPED));
        assertThrows(IllegalArgumentException.class,
                () -> Usuario.crear(id, "ALO-1", "Rita", correo, "", Rol.HUESPED));
    }

    @Test
    @Tag("USU-02")
    void laPoliticaExigeOchoCaracteresConMayusculasYNumeros() {
        assertTrue(PoliticaContrasena.laCumple("PuertaAlSol2026"));
        assertTrue(PoliticaContrasena.laCumple("Abcdefg1"));

        assertFalse(PoliticaContrasena.laCumple("Abcdef1"));
        assertFalse(PoliticaContrasena.laCumple("abcdefg1"));
        assertFalse(PoliticaContrasena.laCumple("Abcdefgh"));
        assertFalse(PoliticaContrasena.laCumple(null));
    }
}
