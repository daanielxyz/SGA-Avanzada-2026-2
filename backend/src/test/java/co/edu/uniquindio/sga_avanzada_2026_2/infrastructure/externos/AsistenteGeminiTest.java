package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AsistenteNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AutorMensaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MensajeConversacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MotivoSinIa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.OrigenRespuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Pregunta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RolConsultante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.SeccionManual;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** El adaptador de Gemini contra un servidor simulado: nunca llama a Google (DEC-127). */
class AsistenteGeminiTest {

    private static final String URL = "https://gemini.prueba/v1beta/models/modelo-de-prueba:generateContent";
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneOffset.UTC);
    private static final SeccionManual LLEGADA = new SeccionManual("reservas-llegada", "Registrar la llegada",
            "/reservas/{codigo}/llegada", Set.of(RolConsultante.RECEPCIONISTA), "Pulsa «Registrar llegada».");

    private MockRestServiceServer servidor;
    private RestClient.Builder cliente;

    @BeforeEach
    void preparar() {
        cliente = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(cliente).build();
    }

    private AsistenteGemini gemini(String clave, int preguntasPorHora) {
        AsistentePropiedades propiedades = new AsistentePropiedades("gemini", clave, "modelo-de-prueba",
                "https://gemini.prueba/v1beta", 2, 256, preguntasPorHora);
        return new AsistenteGemini(propiedades, cliente, JSON, RELOJ);
    }

    private static Consulta consulta(String pregunta) {
        return new Consulta(new UsuarioId("USR-77"), RolConsultante.RECEPCIONISTA, new Pregunta(pregunta),
                "/reservas/RES-2026-00042/llegada",
                List.of(new MensajeConversacion(AutorMensaje.ASISTENTE, "¡Hola! ¿En qué te ayudo?"),
                        new MensajeConversacion(AutorMensaje.PERSONA, "Estoy en la llegada")),
                List.of(LLEGADA), "Hora de entrada: 15:00.");
    }

    /** Lo que responde Gemini: el JSON que se le pidió, dentro del texto del primer candidato. */
    private static String deGemini(String salida) {
        return """
                {"candidates":[{"content":{"role":"model","parts":[{"text":%s}]},"finishReason":"STOP"}],
                 "usageMetadata":{"promptTokenCount":900,"candidatesTokenCount":60,"totalTokenCount":960},
                 "modelVersion":"modelo-de-prueba"}
                """.formatted(JSON.writeValueAsString(salida));
    }

    @Test
    @Tag("DEC-127")
    void deberiaResponderConLaIaYCitarSoloSeccionesQueSeLeMandaron() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(content().string(allOf(
                        containsString("Registrar la llegada"),
                        containsString("Hora de entrada: 15:00."),
                        containsString("/reservas/RES-2026-00042/llegada"),
                        containsString("[documento]"),
                        not(containsString("1094123456")),
                        not(containsString("USR-77")))))
                .andRespond(withSuccess(deGemini("""
                        {"respuesta":"1. Pulsa «Registrar llegada».","fuentes":["reservas-llegada","inventada"]}"""),
                        MediaType.APPLICATION_JSON));

        Respuesta respuesta = gemini("clave-de-prueba", 30).responder(consulta("¿Y si la cédula 1094123456 no sale?"));

        assertEquals(OrigenRespuesta.IA, respuesta.origen());
        assertEquals("1. Pulsa «Registrar llegada».", respuesta.texto());
        assertEquals(List.of("reservas-llegada"), respuesta.fuentes());
        servidor.verify();
    }

    @Test
    @Tag("DEC-127")
    void deberiaEmpezarLaConversacionPorLaPersona() {
        servidor.expect(requestTo(URL))
                .andExpect(content().string(allOf(
                        not(containsString("¿En qué te ayudo?")),
                        containsString("Estoy en la llegada"))))
                .andRespond(withSuccess(deGemini("{\"respuesta\":\"Listo\",\"fuentes\":[]}"),
                        MediaType.APPLICATION_JSON));

        assertEquals("Listo", gemini("clave-de-prueba", 30).responder(consulta("¿Y ahora?")).texto());
        servidor.verify();
    }

    @Test
    @Tag("DEC-127")
    void deberiaAvisarLaCuotaAgotadaCon429() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AsistenteNoDisponibleException sinIa = assertThrows(AsistenteNoDisponibleException.class,
                () -> gemini("clave-de-prueba", 30).responder(consulta("¿Cómo registro la llegada?")));

        assertEquals(MotivoSinIa.CUOTA_AGOTADA, sinIa.motivo());
    }

    @Test
    @Tag("DEC-127")
    void deberiaQuedarNoDisponibleSiGeminiFallaORespondeAlgoIlegible() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withSuccess(deGemini("esto no es JSON"), MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));
        AsistenteGemini gemini = gemini("clave-de-prueba", 30);

        for (int intento = 0; intento < 3; intento++) {
            AsistenteNoDisponibleException sinIa = assertThrows(AsistenteNoDisponibleException.class,
                    () -> gemini.responder(consulta("¿Cómo registro la llegada?")));
            assertEquals(MotivoSinIa.NO_DISPONIBLE, sinIa.motivo());
        }
        servidor.verify();
    }

    @Test
    @Tag("DEC-127")
    void noDeberiaLlamarSinClave() {
        servidor.expect(never(), requestTo(URL));

        AsistenteNoDisponibleException sinIa = assertThrows(AsistenteNoDisponibleException.class,
                () -> gemini(" ", 30).responder(consulta("¿Cómo registro la llegada?")));

        assertEquals(MotivoSinIa.IA_APAGADA, sinIa.motivo());
        servidor.verify();
    }

    @Test
    @Tag("DEC-127")
    void noDeberiaLlamarCuandoLaPersonaGastoSuCupo() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(deGemini("{\"respuesta\":\"Uno\",\"fuentes\":[]}"),
                MediaType.APPLICATION_JSON));
        AsistenteGemini gemini = gemini("clave-de-prueba", 1);
        gemini.responder(consulta("Primera"));

        AsistenteNoDisponibleException sinIa = assertThrows(AsistenteNoDisponibleException.class,
                () -> gemini.responder(consulta("Segunda")));

        assertEquals(MotivoSinIa.LIMITE_PERSONAL, sinIa.motivo());
        servidor.verify();
    }

    @Test
    @Tag("DEC-127")
    void deberiaDevolverElCupoPasadaUnaHora() {
        CupoPreguntas cupo = new CupoPreguntas(2);
        Instant ahora = RELOJ.instant();

        assertEquals(true, cupo.tomar("USR-1", ahora));
        assertEquals(true, cupo.tomar("USR-1", ahora.plusSeconds(60)));
        assertEquals(false, cupo.tomar("USR-1", ahora.plusSeconds(120)));
        assertEquals(true, cupo.tomar("USR-2", ahora.plusSeconds(120)), "El cupo es de cada persona");
        assertEquals(true, cupo.tomar("USR-1", ahora.plusSeconds(3601)));
    }
}
