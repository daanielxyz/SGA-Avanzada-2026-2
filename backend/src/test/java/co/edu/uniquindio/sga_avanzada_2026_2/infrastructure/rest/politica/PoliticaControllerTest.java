package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de la política de cancelación de extremo a extremo; la vigente al empezar es la versión 1 de
 * {@link AlojamientoDePrueba} (B2a).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PoliticaControllerTest {

    private static final String NUEVA = """
            {
              "tramos": [
                {"antelacionMinHoras": 72, "penalizacion": {"base": "VALOR_TOTAL", "porcentaje": 0}},
                {"antelacionMinHoras": 0, "penalizacion": {"base": "ANTICIPO_EXIGIDO", "porcentaje": 100}}
              ],
              "penalizacionNoShow": {"base": "VALOR_TOTAL", "montoFijo": 200000}
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @BeforeEach
    void prepararAlojamiento() {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
    }

    private ResultActions publicar(String json) throws Exception {
        return mvc.perform(post("/api/politicas").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void deberiaConsultarLaPoliticaVigente() throws Exception {
        mvc.perform(get("/api/politicas/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(AlojamientoDePrueba.POLITICA))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.tramos", hasSize(2)))
                .andExpect(jsonPath("$.penalizacionNoShow.porcentaje").value(100));
    }

    @Test
    void deberiaPublicarLaVersionSiguienteQuePasaASerLaVigente() throws Exception {
        publicar(NUEVA)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/politicas/vigente"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.tramos[0].penalizacion.base").value("ANTICIPO_EXIGIDO"))
                .andExpect(jsonPath("$.penalizacionNoShow.montoFijo").value(200000));

        mvc.perform(get("/api/politicas/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void deberiaResponder400SiLaBaseNoExisteOFaltanLosTramos() throws Exception {
        publicar(NUEVA.replace("ANTICIPO_EXIGIDO", "LO_QUE_SEA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem(
                        "tramos[1].penalizacion.base: debe ser VALOR_TOTAL, PAGADO o ANTICIPO_EXIGIDO")));
        publicar("{\"tramos\": [], \"penalizacionNoShow\": {\"base\": \"PAGADO\", \"porcentaje\": 120}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("tramos: debe tener al menos un tramo")))
                .andExpect(jsonPath("$.detalles").value(
                        hasItem("penalizacionNoShow.porcentaje: debe estar entre 0 y 100")));
    }

    @Test
    void deberiaResponder409SiLaPoliticaNoCumpleLasReglas() throws Exception {
        String unSoloTramo = """
                {
                  "tramos": [{"antelacionMinHoras": 0, "penalizacion": {"base": "VALOR_TOTAL", "porcentaje": 50}}],
                  "penalizacionNoShow": {"base": "VALOR_TOTAL", "porcentaje": 100}
                }
                """;

        publicar(unSoloTramo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        publicar(NUEVA.replace("\"montoFijo\": 200000", "\"montoFijo\": 200000, \"porcentaje\": 10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }
}
