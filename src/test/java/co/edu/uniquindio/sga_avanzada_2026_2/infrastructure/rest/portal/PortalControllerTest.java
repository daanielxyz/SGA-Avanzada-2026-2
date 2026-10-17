package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.PasarelaSimulada;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reserva desde el portal y pago en línea de extremo a extremo (B3c · DEC-69), sin sesión, como un visitante del
 * portal. Ana y Luis en el APT-101, dos noches: 400.000; el anticipo del 30 % es 120.000. El administrador de las
 * demás pruebas habilita antes el medio {@code PAGO_EN_LINEA}.
 */
class PortalControllerTest extends PruebaRestDeReservas {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc visitante;

    @BeforeEach
    void prepararPortal() throws Exception {
        visitante = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        mvc.perform(put("/api/alojamiento/medios-pago/PAGO_EN_LINEA/habilitar")).andExpect(status().isOk());
    }

    private ResultActions pedir(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return visitante.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** La reserva de {@link PruebaRestDeReservas}, sin canal y con la hora de llegada. */
    private static String reservaDelPortal() {
        return reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7))
                .replace("  \"canalOrigen\": \"DIRECTO\",\n", "  \"horaEstimadaLlegada\": \"16:00\",\n");
    }

    private String reservar() throws Exception {
        String respuesta = pedir(post("/api/portal/reservas"), reservaDelPortal()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.reserva.codigo");
    }

    private ResultActions pagar(String codigo, String token) throws Exception {
        return pedir(post("/api/portal/reservas/" + codigo + "/pago"), "{\"tokenPago\": \"" + token + "\"}");
    }

    @Test
    @Tag("CU-10")
    @Tag("DEC-69")
    void cualquieraReservaDesdeElPortalYSabeCuantoYHastaCuandoPagar() throws Exception {
        pedir(post("/api/portal/reservas"), reservaDelPortal())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reservas/RES-")))
                .andExpect(jsonPath("$.reserva.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.reserva.canalOrigen").value("PORTAL"))
                .andExpect(jsonPath("$.reserva.horaEstimadaLlegada").value("16:00:00"))
                .andExpect(jsonPath("$.montoAPagar").value(120000))
                .andExpect(jsonPath("$.pagarAntesDe").exists());
    }

    @Test
    @Tag("DEC-69")
    @Tag("RES-15")
    void pagarEnLineaConfirmaLaReservaYDejaElPagoEnElFolio() throws Exception {
        String codigo = reservar();

        pagar(codigo, "tok_visa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        mvc.perform(get("/api/reservas/" + codigo + "/folio"))
                .andExpect(jsonPath("$.pagos", hasSize(1)))
                .andExpect(jsonPath("$.pagos[0].medio").value("PAGO_EN_LINEA"))
                .andExpect(jsonPath("$.pagos[0].monto").value(120000))
                .andExpect(jsonPath("$.saldo").value(280000));
        pagar(codigo, "tok_visa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("DEC-69")
    void unPagoRechazadoEs409YLaReservaSigueEsperandoElPago() throws Exception {
        String codigo = reservar();

        pagar(codigo, PasarelaSimulada.TOKEN_RECHAZADO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PAGO_RECHAZADO"));

        mvc.perform(get("/api/reservas/" + codigo)).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(get("/api/reservas/" + codigo + "/folio")).andExpect(jsonPath("$.pagos").isEmpty());
        pagar(codigo, "tok_visa").andExpect(status().isOk());
    }

    @Test
    @Tag("MPAG-02")
    void sinElPagoEnLineaHabilitadoNoSeReservaNiSeRetieneLaNoche() throws Exception {
        mvc.perform(put("/api/alojamiento/medios-pago/PAGO_EN_LINEA/deshabilitar")).andExpect(status().isOk());

        pedir(post("/api/portal/reservas"), reservaDelPortal())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El alojamiento no tiene habilitado el pago en línea"));

        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", HOY.plusDays(5).toString())
                        .param("salida", HOY.plusDays(7).toString()).param("ocupantes", "2"))
                .andExpect(jsonPath("$[*].codigo").value(hasItem("APT-101")));
    }

    @Test
    @Tag("RN-09")
    @Tag("DEC-68")
    void elPortalPideLaHoraDeLlegadaYElContactoDelTitular() throws Exception {
        String sinHora = reservaDelPortal().replace("  \"horaEstimadaLlegada\": \"16:00\",\n", "")
                .replace(", \"telefono\": \"300 123 4567\"", "");

        pedir(post("/api/portal/reservas"), sinHora)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("horaEstimadaLlegada: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.telefono: es obligatorio")));
        pagar("RES-2026-99999", "tok_visa").andExpect(status().isNotFound());
        pedir(post("/api/portal/reservas/RES-2026-99999/pago"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("tokenPago: es obligatorio")));
    }
}
