package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Formato del medio de pago. Que esté habilitado (MPAG-02) lo decide el Alojamiento: ver AlojamientoTest.
 */
class MedioPagoTest {

    @Test
    @Tag("MPAG-06")
    void deberiaNormalizarElNombre() {
        assertEquals(new MedioPago("EFECTIVO"), new MedioPago("  efectivo "));
    }

    @Test
    @Tag("MPAG-06")
    void deberiaRechazarNombreVacio() {
        assertThrows(ReglaDominioException.class, () -> new MedioPago(" "));
    }
}
