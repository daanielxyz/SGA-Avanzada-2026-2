package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

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
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión de apartamentos por REST de extremo a extremo (B2a): listado, activación, capacidad, estado operativo y
 * bloqueos. El APT-101 (capacidad 4) se crea por su endpoint antes de cada prueba y nace inactivo y
 * PENDIENTE_PREPARACION.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GestionApartamentosControllerTest {

    private static final String APT = "/api/apartamentos/APT-101";

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
        crearApartamento("APT-101");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void crearApartamento(String codigo) throws Exception {
        enviar(post("/api/apartamentos"), """
                {
                  "codigo": "%s",
                  "nombre": "Apartamento",
                  "capacidad": 4,
                  "dormitorios": 2,
                  "imagenes": [{"url": "https://cdn.sga.co/apt.jpg", "principal": true}],
                  "caracteristicas": ["Balcón"]
                }
                """.formatted(codigo)).andExpect(status().isCreated());
    }

    /** Dos temporadas además de la base y la tarifa del APT-101 en las tres: lo que exige la activación. */
    private void dejarListoParaActivar() throws Exception {
        LocalDate hoy = LocalDate.now();
        agregarTemporada("Alta", hoy.plusDays(200), hoy.plusDays(210));
        agregarTemporada("Media", hoy.plusDays(300), hoy.plusDays(310));
        String calendario = mvc.perform(get("/api/temporadas")).andReturn().getResponse().getContentAsString();
        List<String> temporadas = JsonPath.read(calendario, "$.temporadas[*].id");
        for (String temporada : temporadas) {
            enviar(put(APT + "/tarifas/" + temporada), "{\"valorPorOcupante\": 100000}")
                    .andExpect(status().isOk());
        }
    }

    private void agregarTemporada(String nombre, LocalDate inicio, LocalDate fin) throws Exception {
        enviar(post("/api/temporadas"), "{\"nombre\": \"%s\", \"fechaInicio\": \"%s\", \"fechaFin\": \"%s\"}"
                .formatted(nombre, inicio, fin)).andExpect(status().isCreated());
    }

    @Test
    void deberiaListarLosApartamentosDelAlojamientoPorPaginas() throws Exception {
        crearApartamento("APT-102");

        mvc.perform(get("/api/apartamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].codigo").value("APT-101"))
                .andExpect(jsonPath("$.numero").value(0))
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1));
        mvc.perform(get("/api/apartamentos").param("pagina", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    void deberiaResponder400SiLaPaginaEsInvalida() throws Exception {
        mvc.perform(get("/api/apartamentos").param("pagina", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("pagina: no puede ser negativa")));
        mvc.perform(get("/api/apartamentos").param("pagina", "dos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("pagina: tiene un formato inválido")));
    }

    @Test
    void deberiaActivarUnApartamentoConTarifasCompletasYLuegoRetirarlo() throws Exception {
        dejarListoParaActivar();

        mvc.perform(put(APT + "/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(put(APT + "/activar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put(APT + "/retirar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void deberiaResponder409AlActivarSinTarifasY404SiNoExiste() throws Exception {
        mvc.perform(put(APT + "/activar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put("/api/apartamentos/APT-999/activar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void deberiaCambiarLaCapacidadEInformarLasReservasAfectadas() throws Exception {
        enviar(put(APT + "/capacidad"), "{\"capacidad\": 6}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartamento.capacidad").value(6))
                .andExpect(jsonPath("$.reservasAfectadas").isEmpty());
        enviar(put(APT + "/capacidad"), "{\"capacidad\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("capacidad: debe ser al menos 1")));
    }

    @Test
    void deberiaAvanzarElEstadoOperativoPorSusTransiciones() throws Exception {
        enviar(put(APT + "/estado-operativo"), "{\"estado\": \"EN_PREPARACION\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));
        enviar(put(APT + "/estado-operativo"), "{\"estado\": \"PREPARADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    void unEstadoQueNoExisteEs400YUnaTransicionNoPermitidaEs409() throws Exception {
        enviar(put(APT + "/estado-operativo"), "{\"estado\": \"LIMPIO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        enviar(put(APT + "/estado-operativo"), "{\"estado\": \"PREPARADO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void deberiaRegistrarUnBloqueoYLevantarlo() throws Exception {
        LocalDate inicio = LocalDate.now().plusDays(20);
        String bloqueo = "{\"inicio\": \"%s\", \"fin\": \"%s\", \"motivo\": \"Pintura\"}";

        String respuesta = enviar(post(APT + "/bloqueos"), bloqueo.formatted(inicio, inicio.plusDays(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bloqueos", hasSize(1)))
                .andExpect(jsonPath("$.bloqueos[0].motivo").value("Pintura"))
                .andExpect(jsonPath("$.bloqueos[0].vigente").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.bloqueos[0].id");

        mvc.perform(put(APT + "/bloqueos/" + id + "/levantar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueos[0].vigente").value(false));
        mvc.perform(put(APT + "/bloqueos/BLO-99999/levantar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void unBloqueoSinMotivoEs400YConFechasInvertidasEs409() throws Exception {
        LocalDate inicio = LocalDate.now().plusDays(20);

        enviar(post(APT + "/bloqueos"), "{\"inicio\": \"%s\", \"fin\": \"%s\"}".formatted(inicio, inicio.plusDays(3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("motivo: es obligatorio")));
        enviar(post(APT + "/bloqueos"), "{\"inicio\": \"%s\", \"fin\": \"%s\", \"motivo\": \"Pintura\"}"
                .formatted(inicio, inicio.minusDays(3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }
}
