package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Asistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.AsistenteGemini;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.como;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con {@code sga.asistente.modo=gemini} y una clave, pero Gemini inalcanzable (la dirección de las pruebas no existe),
 * el PMS igual responde con el manual: la IA nunca impide operar (DEC-127).
 */
@SpringBootTest(properties = {"sga.asistente.modo=gemini", "sga.asistente.clave=clave-de-prueba"})
@AutoConfigureMockMvc
@Transactional
class AsistenteSinGoogleTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Asistente asistente;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @Test
    @Tag("DEC-127")
    void deberiaResponderConElManualSiGeminiNoContesta() throws Exception {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
        assertInstanceOf(AsistenteGemini.class, asistente);

        mvc.perform(post("/api/asistente/preguntas").with(como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pregunta\": \"¿Cómo registro un pago en el folio?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("LOCAL"))
                .andExpect(jsonPath("$.motivo").value("NO_DISPONIBLE"))
                .andExpect(jsonPath("$.fuentes[0].id").value("folio"));
    }
}
