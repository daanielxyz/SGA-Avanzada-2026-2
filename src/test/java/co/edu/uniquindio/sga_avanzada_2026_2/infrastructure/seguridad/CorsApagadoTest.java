package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sin {@code SGA_CORS_ORIGENES} la API responde como antes de DEC-81: no autoriza a ningún origen, pero tampoco
 * rechaza lo que llega con {@code Origin} a través del proxy de Vite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("DEC-81")
class CorsApagadoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loQueLlegaPorElProxyConSuOrigenSeAtiendeIgual() throws Exception {
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void ningunOrigenQuedaAutorizado() throws Exception {
        mvc.perform(options("/api/reservas")
                        .with(SesionDePrueba.sinSesion())
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
