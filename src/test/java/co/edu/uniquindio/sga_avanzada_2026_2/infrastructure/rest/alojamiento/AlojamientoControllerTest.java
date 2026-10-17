package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints del alojamiento del despliegue de extremo a extremo, sobre {@link AlojamientoDePrueba} (B2a).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlojamientoControllerTest {

    private static final String PARAMETROS = """
            {
              "umbralEdadFacturable": 10,
              "horaEntrada": "14:00",
              "horaSalida": "12:00",
              "tiempoPreparacionHoras": 2,
              "plazoConfirmacionHoras": 48,
              "horaLimiteNoShow": "23:00",
              "anticipoPct": 50,
              "minimoMediosPago": 2,
              "minimoServiciosAdicionales": 1,
              "minimoTemporadas": 2,
              "minimoTramosCancelacion": 2,
              "minimoCapacidadesDistintas": 1,
              "pagoEnLinea": "TOTAL",
              "plazoPagoEnLineaMinutos": 45
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

    private void conAlojamiento() {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void deberiaConsultarElAlojamientoDelDespliegue() throws Exception {
        conAlojamiento();

        mvc.perform(get("/api/alojamiento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ALO-1"))
                .andExpect(jsonPath("$.nombre").value("Puerta al Sol"))
                .andExpect(jsonPath("$.parametros.horaEntrada").value("15:00:00"))
                .andExpect(jsonPath("$.parametros.anticipoPct").value(30))
                .andExpect(jsonPath("$.serviciosAdicionales[0].nombre").value("Desayuno"))
                .andExpect(jsonPath("$.mediosPago", hasSize(2)));
    }

    @Test
    void deberiaResponder404SiElAlojamientoNoSeHaInicializado() throws Exception {
        mvc.perform(get("/api/alojamiento"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void deberiaCambiarLosParametros() throws Exception {
        conAlojamiento();

        enviar(put("/api/alojamiento/parametros"), PARAMETROS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametros.umbralEdadFacturable").value(10))
                .andExpect(jsonPath("$.parametros.horaEntrada").value("14:00:00"))
                .andExpect(jsonPath("$.parametros.plazoConfirmacionHoras").value(48))
                .andExpect(jsonPath("$.parametros.anticipoPct").value(50));
    }

    @Test
    void deberiaResponder400SiLosParametrosTienenMalFormato() throws Exception {
        conAlojamiento();
        String invalido = PARAMETROS.replace("\"anticipoPct\": 50", "\"anticipoPct\": 150")
                .replace("\"horaSalida\": \"12:00\",", "");

        enviar(put("/api/alojamiento/parametros"), invalido)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("anticipoPct: debe estar entre 0 y 100")))
                .andExpect(jsonPath("$.detalles").value(hasItem("horaSalida: es obligatoria")));
        enviar(put("/api/alojamiento/parametros"), PARAMETROS.replace("14:00", "las dos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void deberiaResponder409SiLosMinimosSuperanElCatalogo() throws Exception {
        conAlojamiento();

        enviar(put("/api/alojamiento/parametros"), PARAMETROS.replace("\"minimoMediosPago\": 2",
                "\"minimoMediosPago\": 5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void deberiaCambiarLaUbicacionYRechazarCoordenadasImposibles() throws Exception {
        conAlojamiento();

        enviar(put("/api/alojamiento/ubicacion"), "{\"latitud\": 4.53, \"longitud\": -75.68}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").value(4.53))
                .andExpect(jsonPath("$.longitud").value(-75.68));
        enviar(put("/api/alojamiento/ubicacion"), "{\"latitud\": 120, \"longitud\": -75.68}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put("/api/alojamiento/ubicacion"), "{\"longitud\": -75.68}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("latitud: es obligatoria")));
    }

    @Test
    void deberiaHabilitarYDeshabilitarMediosDePagoRespetandoElMinimo() throws Exception {
        conAlojamiento();

        mvc.perform(put("/api/alojamiento/medios-pago/pse/habilitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediosPago").value(hasItem("PSE")));
        mvc.perform(put("/api/alojamiento/medios-pago/PSE/habilitar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put("/api/alojamiento/medios-pago/PSE/deshabilitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediosPago", hasSize(2)));
        mvc.perform(put("/api/alojamiento/medios-pago/EFECTIVO/deshabilitar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void deberiaAgregarUnServicioCambiarSuValorYDesactivarlo() throws Exception {
        conAlojamiento();

        String respuesta = enviar(post("/api/alojamiento/servicios"),
                "{\"nombre\": \"Parqueadero\", \"generaCargo\": true, \"valor\": 15000}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviciosAdicionales", hasSize(2)))
                .andExpect(jsonPath("$.serviciosAdicionales[1].nombre").value("Parqueadero"))
                .andReturn().getResponse().getContentAsString();
        String parqueadero = JsonPath.read(respuesta, "$.serviciosAdicionales[1].id");

        enviar(put("/api/alojamiento/servicios/" + parqueadero + "/valor"), "{\"valor\": 18000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviciosAdicionales[1].valor").value(18000));
        mvc.perform(put("/api/alojamiento/servicios/" + parqueadero + "/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviciosAdicionales[1].activo").value(false));
        mvc.perform(put("/api/alojamiento/servicios/" + AlojamientoDePrueba.DESAYUNO + "/desactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void deberiaResponder400SiElServicioTieneMalFormato() throws Exception {
        conAlojamiento();

        enviar(post("/api/alojamiento/servicios"), "{\"nombre\": \" \", \"generaCargo\": true, \"valor\": 15000.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("nombre: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(
                        hasItem("valor: debe ser un valor en pesos sin decimales")));
        enviar(put("/api/alojamiento/servicios/" + AlojamientoDePrueba.DESAYUNO + "/valor"), "{\"valor\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("valor: no puede ser negativo")));
    }
}
