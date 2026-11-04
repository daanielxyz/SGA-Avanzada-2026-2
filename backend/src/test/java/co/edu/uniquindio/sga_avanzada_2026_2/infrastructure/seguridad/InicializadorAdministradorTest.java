package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El primer administrador del despliegue (DEC-67), con H2. Las pruebas arrancan sin administrador inicial; aquí se
 * ejecuta el arranque a mano con los valores que llegarían por las variables de entorno.
 */
@SpringBootTest
@Transactional
class InicializadorAdministradorTest {

    private static final String CLAVE = "PuertaAlSol2026";

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private GeneradorCodigos codigos;

    @Autowired
    private PasswordEncoder cifrador;

    @Autowired
    private IniciarSesion iniciarSesion;

    private void arrancarCon(String correo, String clave) {
        new InicializadorAdministrador(usuarios, codigos, cifrador,
                new SeguridadPropiedades(null, new SeguridadPropiedades.Administrador("Marta Díaz", correo, clave)),
                "ALO-1").run(new DefaultApplicationArguments());
    }

    @Test
    @Tag("USU-02")
    @Tag("DEC-67")
    void deberiaCrearElAdministradorConLaClaveCifrada() {
        arrancarCon(" Marta@PuertaAlSol.co ", CLAVE);

        Usuario administrador = usuarios.findByCorreo("marta@puertaalsol.co").orElseThrow();
        assertEquals(Rol.ADMINISTRADOR, administrador.rol());
        assertEquals("Marta Díaz", administrador.nombre());
        assertTrue(administrador.id().matches("USR-\\d+"));
        assertNotEquals(CLAVE, administrador.contrasena());
        assertTrue(cifrador.matches(CLAVE, administrador.contrasena()));
        assertEquals(administrador.id(), iniciarSesion.ejecutar("marta@puertaalsol.co", CLAVE).usuario());
    }

    @Test
    @Tag("DEC-67")
    void siYaHayUnAdministradorNoCreaOtro() {
        arrancarCon("marta@puertaalsol.co", CLAVE);

        arrancarCon("otra@puertaalsol.co", CLAVE);

        assertEquals(1, usuarios.count());
    }

    @Test
    @Tag("USU-02")
    void unaClaveQueNoCumpleLaPoliticaDetieneElArranque() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> arrancarCon("marta@puertaalsol.co", "puertaalsol"));

        assertTrue(error.getMessage().contains("mínimo 8 caracteres"));
        assertEquals(0, usuarios.count());
    }

    @Test
    @Tag("DEC-67")
    void sinCorreoOSinClaveNoCreaANadie() {
        arrancarCon("", CLAVE);
        arrancarCon("marta@puertaalsol.co", null);

        assertEquals(0, usuarios.count());
    }
}
