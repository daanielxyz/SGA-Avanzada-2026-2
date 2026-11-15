package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Asistente;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AsistenteNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.AutorMensaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Consulta;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MensajeConversacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.MotivoSinIa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.Respuesta;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link Asistente} con la API de Gemini de Google AI Studio (DEC-127), activo con {@code sga.asistente.modo=gemini}.
 * Llama a {@code POST {url}/models/{modelo}:generateContent} con el {@code RestClient} de Spring, sin el SDK de Google.
 * Al modelo solo le llegan el manual del rol, la configuración del hotel y la conversación ya enmascarada; nunca quién
 * pregunta. Si no hay clave, se acaba el cupo de la persona o la cuota del proyecto, Gemini falla o no responde a
 * tiempo, lanza {@link AsistenteNoDisponibleException} y el caso de uso responde con el manual.
 */
@Component
@Primary
@ConditionalOnProperty(name = "sga.asistente.modo", havingValue = "gemini")
@EnableConfigurationProperties(AsistentePropiedades.class)
public class AsistenteGemini implements Asistente {

    private static final Logger LOG = LoggerFactory.getLogger(AsistenteGemini.class);

    /** La forma de la respuesta que se le pide al modelo: el texto y las secciones citadas. */
    private static final Map<String, Object> FORMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "respuesta", Map.of("type", "string"),
                    "fuentes", Map.of("type", "array", "items", Map.of("type", "string"))),
            "required", List.of("respuesta", "fuentes"));

    private final AsistentePropiedades propiedades;
    private final RestClient cliente;
    private final ObjectMapper json;
    private final Clock reloj;
    private final CupoPreguntas cupo;

    @Autowired
    public AsistenteGemini(AsistentePropiedades propiedades, ObjectMapper json, Clock reloj) {
        this(propiedades, RestClient.builder().requestFactory(fabrica(propiedades)), json, reloj);
    }

    AsistenteGemini(AsistentePropiedades propiedades, RestClient.Builder cliente, ObjectMapper json, Clock reloj) {
        this.propiedades = propiedades;
        this.cliente = cliente.baseUrl(propiedades.url()).build();
        this.json = json;
        this.reloj = reloj;
        this.cupo = new CupoPreguntas(propiedades.preguntasPorHora());
        if (!propiedades.tieneClave()) {
            LOG.warn("sga.asistente.modo=gemini sin SGA_IA_CLAVE: el asistente responderá con el manual");
        }
    }

    // DEC-127
    @Override
    public Respuesta responder(Consulta consulta) {
        if (!propiedades.tieneClave()) {
            throw new AsistenteNoDisponibleException(MotivoSinIa.IA_APAGADA, "Falta SGA_IA_CLAVE");
        }
        if (!cupo.tomar(consulta.solicitante().valor(), reloj.instant())) {
            throw new AsistenteNoDisponibleException(MotivoSinIa.LIMITE_PERSONAL, "Sin cupo de preguntas por hora");
        }
        RespuestaGemini respuesta = llamar(peticion(consulta));
        return leer(respuesta, consulta);
    }

    private Peticion peticion(Consulta consulta) {
        return new Peticion(
                new Contenido(null, List.of(new Parte(InstruccionesAsistente.para(consulta)))),
                conversacion(consulta),
                new Configuracion(propiedades.maximoTokensRespuesta(), MediaType.APPLICATION_JSON_VALUE, FORMA));
    }

    /** Gemini espera que la conversación empiece por la persona: lo que el asistente dijo antes de eso se omite. */
    private static List<Contenido> conversacion(Consulta consulta) {
        List<Contenido> contenidos = new ArrayList<>();
        for (MensajeConversacion mensaje : consulta.historial()) {
            if (!contenidos.isEmpty() || mensaje.autor() == AutorMensaje.PERSONA) {
                contenidos.add(new Contenido(mensaje.autor() == AutorMensaje.PERSONA ? "user" : "model",
                        List.of(new Parte(mensaje.texto()))));
            }
        }
        contenidos.add(new Contenido("user", List.of(new Parte(consulta.pregunta().texto()))));
        return contenidos;
    }

    private RespuestaGemini llamar(Peticion peticion) {
        try {
            String cuerpo = cliente.post()
                    .uri("/models/{modelo}:generateContent", propiedades.modelo())
                    // La clave va en la cabecera, no en la dirección, para que no quede escrita en ningún log
                    .header("x-goog-api-key", propiedades.clave())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(peticion))
                    .retrieve()
                    .body(String.class);
            return json.readValue(Objects.requireNonNullElse(cuerpo, "{}"), RespuestaGemini.class);
        } catch (RestClientResponseException fallo) {
            LOG.warn("Gemini respondió {} al asistente", fallo.getStatusCode().value());
            MotivoSinIa motivo = fallo.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                    ? MotivoSinIa.CUOTA_AGOTADA : MotivoSinIa.NO_DISPONIBLE;
            throw new AsistenteNoDisponibleException(motivo, "Gemini respondió " + fallo.getStatusCode().value());
        } catch (RuntimeException fallo) {
            LOG.warn("Gemini no respondió al asistente: {}", fallo.getClass().getSimpleName());
            throw new AsistenteNoDisponibleException(MotivoSinIa.NO_DISPONIBLE, fallo.getClass().getSimpleName());
        }
    }

    /** El texto del modelo es el JSON de {@link #FORMA}; solo se citan secciones que de verdad se le mandaron. */
    private Respuesta leer(RespuestaGemini respuesta, Consulta consulta) {
        registrarUso(respuesta.usageMetadata());
        String texto = respuesta.texto();
        try {
            Salida salida = json.readValue(texto, Salida.class);
            if (salida.respuesta() == null || salida.respuesta().isBlank()) {
                throw new AsistenteNoDisponibleException(MotivoSinIa.NO_DISPONIBLE, "Gemini no dio respuesta");
            }
            List<String> fuentes = salida.fuentes() == null ? List.of()
                    : salida.fuentes().stream().filter(consulta::incluye).toList();
            return Respuesta.deIa(salida.respuesta().strip(), fuentes);
        } catch (JacksonException | IllegalArgumentException fallo) {
            LOG.warn("Gemini respondió algo que no se pudo leer (fin: {})", respuesta.finDelPrimero());
            throw new AsistenteNoDisponibleException(MotivoSinIa.NO_DISPONIBLE, "Respuesta ilegible de Gemini");
        }
    }

    private static void registrarUso(Uso uso) {
        if (uso != null) {
            LOG.info("Asistente con Gemini: {} tokens de entrada, {} de salida, {} en total",
                    uso.promptTokenCount(), uso.candidatesTokenCount(), uso.totalTokenCount());
        }
    }

    private static JdkClientHttpRequestFactory fabrica(AsistentePropiedades propiedades) {
        Duration limite = Duration.ofSeconds(propiedades.tiempoLimiteSegundos());
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(limite).build());
        fabrica.setReadTimeout(limite);
        return fabrica;
    }

    // --- La forma de la API de Gemini (generateContent), solo con lo que se usa ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Peticion(Contenido systemInstruction, List<Contenido> contents, Configuracion generationConfig) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Contenido(String role, List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) {
    }

    record Configuracion(int maxOutputTokens, String responseMimeType, Map<String, Object> responseJsonSchema) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaGemini(List<Candidato> candidates, Uso usageMetadata) {

        /** El texto del primer candidato, con todas sus partes. */
        String texto() {
            if (candidates == null || candidates.isEmpty() || candidates.getFirst().content() == null
                    || candidates.getFirst().content().parts() == null) {
                throw new AsistenteNoDisponibleException(MotivoSinIa.NO_DISPONIBLE, "Gemini no devolvió texto");
            }
            return candidates.getFirst().content().parts().stream()
                    .map(Parte::text)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());
        }

        String finDelPrimero() {
            return candidates == null || candidates.isEmpty() ? "sin candidatos" : candidates.getFirst().finishReason();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Contenido content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Uso(Integer promptTokenCount, Integer candidatesTokenCount, Integer totalTokenCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Salida(String respuesta, List<String> fuentes) {
    }
}
