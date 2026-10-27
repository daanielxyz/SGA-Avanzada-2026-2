package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sin {@code SGA_DATOS_DEMO=true} (lo normal en un servidor real) no se cargan datos de ejemplo (DEC-81).
 */
@SpringBootTest
class DatosDemoApagadosTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void apagadosNoExisteElCargador() {
        assertTrue(contexto.getBeansOfType(CargadorDatosDemo.class).isEmpty());
    }
}
