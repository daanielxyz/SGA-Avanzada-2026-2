package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con {@code SGA_DOCUMENTACION_API=true} el contrato OpenAPI y Swagger UI se ven sin sesión (DEC-79).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class DocumentacionApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elContratoSeVeSinSesionYDescribeLosEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SGA · API de Puerta al Sol"))
                .andExpect(jsonPath("$.paths['/api/cotizaciones'].post").exists())
                .andExpect(jsonPath("$.paths['/api/portal/reservas'].post").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.codigo").exists());
    }

    @Test
    void cadaEndpointDiceComoLleganSusErrores() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/cotizaciones'].post.responses.default.content"
                        + "['application/json'].schema.$ref").value("#/components/schemas/ErrorResponse"));
    }

    @Test
    void ofreceLasDosFormasDeEntrar() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.token.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.claveCanal.name").value("X-Canal-Clave"));
    }

    @Test
    void soloDocumentaLaApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/v3/api-docs']").doesNotExist())
                .andExpect(jsonPath("$.paths['/error']").doesNotExist());
    }

    @Test
    void swaggerUiSeAbreSinSesion() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
