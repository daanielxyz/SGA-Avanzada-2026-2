package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin {@code SGA_DOCUMENTACION_API=true} (lo normal en un servidor real) no se publica el contrato (DEC-79).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacionApiApagadaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void apagadaNoExisteNiElContratoNiSwaggerUi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
