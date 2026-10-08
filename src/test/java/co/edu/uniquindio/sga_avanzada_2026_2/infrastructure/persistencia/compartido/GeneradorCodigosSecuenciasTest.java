package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.compartido;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las secuencias de V8 existen en H2 y cada serie entrega códigos con su prefijo, sin repetir.
 */
@DataJpaTest
@Import(GeneradorCodigosSecuencias.class)
class GeneradorCodigosSecuenciasTest {

    @Autowired
    private GeneradorCodigosSecuencias generador;

    @Test
    void cadaSerieEntregaCodigosConSuPrefijoSinRepetirse() {
        String primero = generador.siguiente(SerieCodigo.TEMPORADA);
        String segundo = generador.siguiente(SerieCodigo.TEMPORADA);

        assertTrue(primero.matches("TEM-\\d+"));
        assertEquals(Long.parseLong(primero.substring(4)) + 1, Long.parseLong(segundo.substring(4)));
    }

    @Test
    void todasLasSeriesTienenSuSecuencia() {
        List<String> codigos = Arrays.stream(SerieCodigo.values()).map(generador::siguiente).toList();

        assertEquals(SerieCodigo.values().length, codigos.stream().distinct().count());
        assertTrue(codigos.stream().allMatch(c -> c.matches("[A-Z]{3}-\\d+")));
    }
}
