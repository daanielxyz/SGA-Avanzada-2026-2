package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El proveedor y el monitor de disponibilidad preguntan por la salud del servicio sin token (DEC-121). Solo se
 * publica {@code /actuator/health}, y sin detalles: no dice qué base usa ni qué versión corre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("DEC-121")
class SaludServicioTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laSaludRespondeSinTokenYSinDetalles() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("jdbc"))));
    }

    @Test
    void elRestoDelActuatorNoSePublica() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator")).andExpect(status().is4xxClientError());
    }
}
