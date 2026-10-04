package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DormitorioTest {

    @Test
    @Tag("DOR-02")
    void deberiaCrearDormitorioValido() {
        assertEquals(2, new Dormitorio(2).cantidad());
    }

    @Test
    @Tag("DOR-02")
    void lanzarExcepcionSiLaConfiguracionDelDormitorioEsInvalida() {
        assertThrows(ReglaDominioException.class, () -> new Dormitorio(0));
    }
}
