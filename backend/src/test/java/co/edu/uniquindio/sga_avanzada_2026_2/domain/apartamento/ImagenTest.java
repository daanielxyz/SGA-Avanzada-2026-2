package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImagenTest {

    @Test
    @Tag("IMG-04")
    void deberiaCrearImagenConUrlValida() {
        assertEquals("https://cdn.sga.co/apt-101.jpg", new Imagen(" https://cdn.sga.co/apt-101.jpg ", true).url());
    }

    @Test
    @Tag("IMG-04")
    void deberiaRechazarImagenConUrlInvalida() {
        assertThrows(ReglaDominioException.class, () -> new Imagen("", false));
        assertThrows(ReglaDominioException.class, () -> new Imagen("apt-101.jpg", false));
        assertThrows(ReglaDominioException.class, () -> new Imagen("ftp://cdn.sga.co/a.jpg", false));
        assertThrows(ReglaDominioException.class, () -> new Imagen("https://cdn sga.co/a.jpg", false));
    }
}
