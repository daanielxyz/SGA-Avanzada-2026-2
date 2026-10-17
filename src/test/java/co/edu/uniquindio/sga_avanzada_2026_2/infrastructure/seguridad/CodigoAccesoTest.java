package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las reglas del PIN del huésped (DEC-68): 15 minutos, un solo uso y hasta el quinto intento fallido.
 */
class CodigoAccesoTest {

    private static final PasswordEncoder CIFRADOR = new BCryptPasswordEncoder(4);
    private static final LocalDateTime EMITIDO = LocalDateTime.of(2026, 10, 1, 10, 0);

    private static CodigoAcceso pin123456() {
        return CodigoAcceso.emitir("TIT-1", PropositoCodigo.INICIO_SESION, CIFRADOR.encode("123456"), null, EMITIDO,
                EMITIDO.plusMinutes(15));
    }

    @Test
    @Tag("DEC-68")
    void elPinCorrectoValeUnaSolaVez() {
        CodigoAcceso codigo = pin123456();

        assertTrue(codigo.verificar("123456", EMITIDO.plusMinutes(14), CIFRADOR));
        assertTrue(codigo.usado());
        assertFalse(codigo.verificar("123456", EMITIDO.plusMinutes(14), CIFRADOR));
    }

    @Test
    @Tag("DEC-68")
    void alQuintoIntentoFallidoNiElCorrectoVale() {
        CodigoAcceso codigo = pin123456();

        for (int i = 0; i < 4; i++) {
            assertFalse(codigo.verificar("654321", EMITIDO, CIFRADOR));
        }
        assertTrue(pin123456().verificar("123456", EMITIDO, CIFRADOR));
        assertFalse(codigo.verificar("654321", EMITIDO, CIFRADOR));

        assertEquals(CodigoAcceso.MAXIMO_INTENTOS, codigo.intentos());
        assertFalse(codigo.verificar("123456", EMITIDO, CIFRADOR));
        assertFalse(codigo.usado());
    }

    @Test
    @Tag("DEC-68")
    void aLosQuinceMinutosElPinVence() {
        assertFalse(pin123456().verificar("123456", EMITIDO.plusMinutes(15), CIFRADOR));
    }
}
