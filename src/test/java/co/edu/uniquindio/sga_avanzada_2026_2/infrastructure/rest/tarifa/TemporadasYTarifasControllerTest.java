package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Temporadas y tarifas por REST de extremo a extremo (B2a). El calendario empieza solo con la temporada base de
 * {@link AlojamientoDePrueba} y el APT-101 está inactivo y sin tarifas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TemporadasYTarifasControllerTest {

    private static final LocalDate INICIO = LocalDate.now().plusDays(200);
    private static final String TARIFAS = "/api/apartamentos/APT-101/tarifas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @BeforeEach
    void preparar() throws Exception {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
        enviar(post("/api/apartamentos"), """
                {
                  "codigo": "APT-101",
                  "nombre": "Apartamento 101",
                  "capacidad": 4,
                  "dormitorios": 2,
                  "imagenes": [{"url": "https://cdn.sga.co/apt.jpg", "principal": true}],
                  "caracteristicas": ["Balcón"]
                }
                """).andExpect(status().isCreated());
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String temporada(String nombre, LocalDate inicio, LocalDate fin) {
        return "{\"nombre\": \"%s\", \"fechaInicio\": \"%s\", \"fechaFin\": \"%s\", \"estanciaMinimaNoches\": 2}"
                .formatted(nombre, inicio, fin);
    }

    private String agregarAlta() throws Exception {
        String respuesta = enviar(post("/api/temporadas"), temporada("Alta", INICIO, INICIO.plusDays(10)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.temporada.id");
    }

    @Test
    void deberiaConsultarElCalendarioConLaTemporadaBase() throws Exception {
        mvc.perform(get("/api/temporadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporadas", hasSize(1)))
                .andExpect(jsonPath("$.temporadas[0].id").value(AlojamientoDePrueba.TEMPORADA_BASE))
                .andExpect(jsonPath("$.temporadas[0].esBase").value(true));
    }

    @Test
    void deberiaAgregarUnaTemporadaYRechazarLaQueSeSolapa() throws Exception {
        enviar(post("/api/temporadas"), temporada("Alta", INICIO, INICIO.plusDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.temporada.nombre").value("Alta"))
                .andExpect(jsonPath("$.temporada.fechaInicio").value(INICIO.toString()))
                .andExpect(jsonPath("$.temporada.estanciaMinimaNoches").value(2))
                .andExpect(jsonPath("$.temporada.activa").value(true))
                .andExpect(jsonPath("$.tarifas").isEmpty());

        enviar(post("/api/temporadas"), temporada("Puente", INICIO.plusDays(5), INICIO.plusDays(15)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(get("/api/temporadas"))
                .andExpect(jsonPath("$.temporadas", hasSize(2)));
    }

    @Test
    void deberiaResponder400SiLaTemporadaTieneMalFormato() throws Exception {
        enviar(post("/api/temporadas"), "{\"fechaInicio\": \"%s\", \"estanciaMinimaNoches\": -1}".formatted(INICIO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("nombre: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("fechaFin: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(hasItem("estanciaMinimaNoches: no puede ser negativa")));
        enviar(post("/api/temporadas"), temporada("Alta", INICIO, INICIO).replace(INICIO.toString(), "mañana"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void deberiaCambiarLasFechasDeUnaTemporadaYDesactivarla() throws Exception {
        String alta = agregarAlta();
        String fechas = "{\"fechaInicio\": \"%s\", \"fechaFin\": \"%s\"}";

        enviar(put("/api/temporadas/" + alta + "/fechas"), fechas.formatted(INICIO.plusDays(1), INICIO.plusDays(8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporadas[1].fechaFin").value(INICIO.plusDays(8).toString()));
        enviar(put("/api/temporadas/" + alta + "/fechas"), fechas.formatted(INICIO.plusDays(8), INICIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put("/api/temporadas/" + alta + "/fechas"), "{\"fechaInicio\": \"%s\"}".formatted(INICIO))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/temporadas/" + alta + "/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporadas[1].activa").value(false));
        mvc.perform(put("/api/temporadas/TEM-99999/desactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void deberiaDefinirLaTarifaYPublicarSuVersionSiguiente() throws Exception {
        String base = TARIFAS + "/" + AlojamientoDePrueba.TEMPORADA_BASE;

        enviar(put(base), "{\"valorPorOcupante\": 100000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartamento").value("APT-101"))
                .andExpect(jsonPath("$.temporada").value(AlojamientoDePrueba.TEMPORADA_BASE))
                .andExpect(jsonPath("$.version").value(1));
        enviar(put(base), "{\"valorPorOcupante\": 120000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(get(TARIFAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].valorPorOcupante").value(120000));
    }

    @Test
    void unaTarifaMalFormadaEs400YDefinirlaEnUnApartamentoInexistenteEs404() throws Exception {
        String base = TARIFAS + "/" + AlojamientoDePrueba.TEMPORADA_BASE;

        enviar(put(base), "{\"valorPorOcupante\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("valorPorOcupante: debe ser mayor que cero")));
        enviar(put(base), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("valorPorOcupante: es obligatorio")));
        enviar(put(TARIFAS + "/TEM-99999"), "{\"valorPorOcupante\": 100000}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put("/api/apartamentos/APT-999/tarifas/" + AlojamientoDePrueba.TEMPORADA_BASE),
                "{\"valorPorOcupante\": 100000}")
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/apartamentos/APT-999/tarifas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
