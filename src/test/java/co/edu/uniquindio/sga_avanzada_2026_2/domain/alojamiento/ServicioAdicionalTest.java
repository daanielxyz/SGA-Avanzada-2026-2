package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioAdicionalTest {

    private static ServicioAdicional desayuno(boolean activo) {
        return new ServicioAdicional(new ServicioAdicionalId("SRV-1"), " Desayuno ", true, Dinero.de(25_000), activo);
    }

    @Test
    @Tag("SERV-01")
    void deberiaCrearServicioConNombreYValor() {
        ServicioAdicional servicio = desayuno(true);

        assertEquals("Desayuno", servicio.nombre());
        assertEquals(Dinero.de(25_000), servicio.valor());
    }

    @Test
    @Tag("SERV-01")
    void deberiaRechazarServicioSinNombre() {
        assertThrows(ReglaDominioException.class,
                () -> new ServicioAdicional(new ServicioAdicionalId("SRV-1"), " ", true, Dinero.CERO, true));
    }

    @Test
    @Tag("SERV-04")
    void deberiaDesactivarseSinBorrarse() {
        ServicioAdicional servicio = desayuno(true);

        servicio.desactivar();

        assertFalse(servicio.activo());
    }

    @Test
    @Tag("SERV-04")
    void deberiaRechazarDesactivarUnServicioInactivo() {
        assertThrows(ReglaDominioException.class, () -> desayuno(false).desactivar());
    }
}
