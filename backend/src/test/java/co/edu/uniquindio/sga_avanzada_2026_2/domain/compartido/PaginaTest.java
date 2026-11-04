package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginaTest {

    @Test
    void deberiaCalcularElTotalDePaginasYConvertirSuContenido() {
        Pagina<Integer> pagina = new Pagina<>(List.of(1, 2), 2, Pagina.TAMANO, 22);

        Pagina<String> convertida = pagina.map(n -> "N" + n);

        assertEquals(3, pagina.totalPaginas());
        assertEquals(List.of("N1", "N2"), convertida.contenido());
        assertEquals(2, convertida.numero());
        assertEquals(22, convertida.totalElementos());
    }

    @Test
    void noDeberiaAceptarUnaPaginaInvalida() {
        assertThrows(ReglaDominioException.class, () -> new Pagina<>(List.of(), -1, 10, 0));
        assertThrows(ReglaDominioException.class, () -> new Pagina<>(List.of(), 0, 0, 0));
    }
}
