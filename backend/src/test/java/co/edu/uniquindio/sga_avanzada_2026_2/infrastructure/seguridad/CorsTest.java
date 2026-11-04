package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con {@code SGA_CORS_ORIGENES} los frontends publicados aparte llaman a la API desde su origen; cualquier otro, no
 * (DEC-82).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sga.cors.origenes= https://reservas.ejemplo.com , https://pms.ejemplo.com")
@Tag("DEC-82")
class CorsTest {

    private static final String WEB = "https://reservas.ejemplo.com";
    private static final String PMS = "https://pms.ejemplo.com";

    @Autowired
    private MockMvc mvc;

    @Test
    void laConsultaPreviaDeUnFrontendPasaSinTokenYDiceQuePuedeEnviarlo() throws Exception {
        mvc.perform(options("/api/reservas/RES-2026-00001/cancelar")
                        .with(SesionDePrueba.sinSesion())
                        .header(HttpHeaders.ORIGIN, PMS)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization, content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PMS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void laRespuestaLeDiceAlFrontendQuePuedeLeerlaYVerElLocation() throws Exception {
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.ORIGIN, WEB))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    void unOrigenQueNoEstaConfiguradoSeRechaza() throws Exception {
        mvc.perform(options("/api/reservas")
                        .with(SesionDePrueba.sinSesion())
                        .header(HttpHeaders.ORIGIN, "https://sitio-ajeno.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.ORIGIN, "https://sitio-ajeno.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void niPatchNiDeleteSePermitenDesdeElNavegador() throws Exception {
        for (String metodo : new String[]{"PATCH", "DELETE"}) {
            mvc.perform(options("/api/reservas/RES-2026-00001")
                            .with(SesionDePrueba.sinSesion())
                            .header(HttpHeaders.ORIGIN, PMS)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo))
                    .andExpect(status().isForbidden());
        }
    }
}
