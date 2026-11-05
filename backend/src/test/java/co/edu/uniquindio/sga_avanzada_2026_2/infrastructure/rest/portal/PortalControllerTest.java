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

    /** La reserva de {@link PruebaRestDeReservas}, sin canal. */
    private static String reservaDelPortal() {
        return reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7))
                .replace("  \"canalOrigen\": \"DIRECTO\",\n", "");
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
                .andExpect(jsonPath("$.reserva.horaEstimadaLlegada").value("15:00:00"))
                .andExpect(jsonPath("$.montoAPagar").value(120000))
                .andExpect(jsonPath("$.cargosAdicionales").value(0))
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
    void elPagoNoRevelaLosDatosDeLaReservaAQuienSoloTieneElCodigo() throws Exception {
        pagar(reservar(), "tok_visa")
                .andExpect(jsonPath("$.codigo").exists())
                .andExpect(jsonPath("$.apartamento").value("APT-101"))
                .andExpect(jsonPath("$.total").value(400000))
                .andExpect(jsonPath("$.ocupantes").doesNotExist())
                .andExpect(jsonPath("$.titularId").doesNotExist())
                .andExpect(jsonPath("$.titularNombre").doesNotExist());
    }

    @Test
    @Tag("DEC-41")
    void cualquieraVeLaPoliticaDeCancelacionAntesDePagar() throws Exception {
        visitante.perform(get("/api/portal/politica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tramos").isNotEmpty())
                .andExpect(jsonPath("$.tramos[0].antelacionMinHoras").exists())
                .andExpect(jsonPath("$.tramos[0].penalizacion.base").exists())
                .andExpect(jsonPath("$.penalizacionNoShow.base").exists())
                .andExpect(jsonPath("$.alojamientoId").doesNotExist());
    }

    @Test
    @Tag("RP-02")
    @Tag("RP-03")
    void elPortalDeclaraVehiculosYMascotasYSusReglasSeAplican() throws Exception {
        String conExtras = reservaDelPortal().replaceFirst("\\{", "{\n  \"vehiculos\": 1,\n  \"mascotas\": 0,");
        pedir(post("/api/portal/reservas"), conExtras.replace("\"vehiculos\": 1", "\"vehiculos\": -1"))
                .andExpect(status().isBadRequest());
        pedir(post("/api/portal/reservas"), conExtras.replace("\"vehiculos\": 1", "\"vehiculos\": 99"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        pedir(post("/api/portal/reservas"), conExtras.replace("\"mascotas\": 0", "\"mascotas\": 99"))
                .andExpect(status().isConflict());
    }

    @Test
    @Tag("RP-02")
    @Tag("RP-03")
    void conParqueaderoYMascotasPermitidosLaReservaLosGuarda() throws Exception {
        mvc.perform(put("/api/alojamiento/parametros").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "umbralEdadFacturable": 12, "horaEntrada": "15:00", "horaSalida": "11:00",
                  "tiempoPreparacionHoras": 3, "plazoConfirmacionHoras": 24, "horaLimiteNoShow": "22:00",
                  "anticipoPct": 30, "minimoMediosPago": 1, "minimoServiciosAdicionales": 1, "minimoTemporadas": 0,
                  "minimoTramosCancelacion": 1, "minimoCapacidadesDistintas": 1, "pagoEnLinea": "ANTICIPO",
                  "plazoPagoEnLineaMinutos": 30, "cupoParqueadero": 2, "aceptaMascotas": true, "limiteMascotas": 1,
                  "cargoMascota": 30000
                }
                """)).andExpect(status().isOk());

        pedir(post("/api/portal/reservas"),
                reservaDelPortal().replaceFirst("\\{", "{\n  \"vehiculos\": 1,\n  \"mascotas\": 1,"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserva.vehiculos").value(1))
                .andExpect(jsonPath("$.reserva.mascotas").value(1))
                .andExpect(jsonPath("$.cargosAdicionales").value(30000));
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
                .andExpect(jsonPath("$.contenido[*].codigo").value(hasItem("APT-101")));
    }

    @Test
    @Tag("RN-09")
    void laHoraDeLlegadaEsLaDeEntradaDelHotelAunqueElPedidoTraigaOtra() throws Exception {
        pedir(post("/api/portal/reservas"),
                reservaDelPortal().replaceFirst("\\{", "{\n  \"horaEstimadaLlegada\": \"23:00\","))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserva.horaEstimadaLlegada").value("15:00:00"));
    }

    @Test
    @Tag("DEC-68")
    void elPortalPideElContactoDelTitular() throws Exception {
        String sinTelefono = reservaDelPortal().replace(", \"telefono\": \"300 123 4567\"", "");

        pedir(post("/api/portal/reservas"), sinTelefono)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.telefono: es obligatorio")));
        pagar("RES-2026-99999", "tok_visa").andExpect(status().isNotFound());
        pedir(post("/api/portal/reservas/RES-2026-99999/pago"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("tokenPago: es obligatorio")));
    }
}
