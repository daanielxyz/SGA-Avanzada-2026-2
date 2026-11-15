package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.como;
import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.sinSesion;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/asistente/preguntas} de extremo a extremo con el manual real, en modo local como en las pruebas:
 * nunca sale a Google (DEC-127).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AsistenteControllerTest {

    private static final String RUTA = "/api/asistente/preguntas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @BeforeEach
    void conAlojamiento() {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
    }

    private ResultActions preguntar(RequestPostProcessor quien, String json) throws Exception {
        return mvc.perform(post(RUTA).with(quien).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @Tag("DEC-127")
    void deberiaResponderARecepcionConElManual() throws Exception {
        preguntar(como(Rol.RECEPCIONISTA), """
                {"pregunta": "¿Cómo declaro el no-show de una reserva?", "pantalla": "/reservas/RES-2026-00042",
                 "historial": [{"autor": "PERSONA", "texto": "Hola"}, {"autor": "ASISTENTE", "texto": "¿En qué te ayudo?"}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("LOCAL"))
                .andExpect(jsonPath("$.motivo").value("IA_APAGADA"))
                .andExpect(jsonPath("$.aviso").isNotEmpty())
                .andExpect(jsonPath("$.fuentes[0].id").value("reservas-cancelar-noshow"))
                .andExpect(jsonPath("$.fuentes[0].pantalla").value("/reservas/{codigo}"))
                .andExpect(jsonPath("$.respuesta").isNotEmpty());
    }

    @Test
    @Tag("DEC-127")
    void noDeberiaResponderleAServicioConLoDeRecepcion() throws Exception {
        preguntar(como(Rol.PERSONAL_SERVICIO), "{\"pregunta\": \"¿Cómo declaro el no-show de una reserva?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuentes[*].id", everyItem(not(startsWith("reservas")))));
        preguntar(como(Rol.PERSONAL_SERVICIO), "{\"pregunta\": \"¿Cómo marco un apartamento como preparado?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuentes[*].id", hasItem("preparacion")));
    }

    @Test
    @Tag("DEC-127")
    void deberiaSerSoloParaElPersonal() throws Exception {
        preguntar(como(Rol.HUESPED), "{\"pregunta\": \"Hola\"}").andExpect(status().isForbidden());
        preguntar(sinSesion(), "{\"pregunta\": \"Hola\"}").andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("DEC-127")
    void deberiaRechazarUnaPreguntaMalFormada() throws Exception {
        preguntar(como(Rol.ADMINISTRADOR), "{\"pregunta\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        preguntar(como(Rol.ADMINISTRADOR), "{\"pregunta\": \"" + "a".repeat(1001) + "\"}")
                .andExpect(status().isBadRequest());
        preguntar(como(Rol.ADMINISTRADOR), """
                {"pregunta": "Hola", "historial": [{"autor": "OTRO", "texto": "x"}]}""")
                .andExpect(status().isBadRequest());
    }
}
