package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AutenticadorCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Canales, contrato del canal externo, conflictos y bitácora por REST (B2c), sobre los dos apartamentos de
 * {@link PruebaRestDeReservas}. Las reservas externas entran dentro de 10 días.
 */
class CanalesControllerTest extends PruebaRestDeReservas {

    private static final LocalDate ENTRADA = HOY.plusDays(10);
    private static final String CLAVE_BOOKING = "clave-de-prueba-booking";

    private String canalExterno() throws Exception {
        String respuesta = enviar(post("/api/canales"),
                "{\"nombre\": \"Booking\", \"tipo\": \"EXTERNO\", \"referenciaCredencial\": \"BOOKING_API_KEY\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    /** Eva (titular, CC 2077) sola, del día {@code desde} al {@code hasta} contados desde la entrada. */
    private static String pedido(String idExterno, int desde, int hasta) {
        return """
                {
                  "idExterno": "%s",
                  "apartamento": "APT-101",
                  "entrada": "%s",
                  "salida": "%s",
                  "titular": {"nombre": "Eva Mora", "tipoDocumento": "CC", "numeroDocumento": "2077",
                              "telefono": "300 123 4567"},
                  "ocupantes": [
                    {"nombre": "Eva Mora", "fechaNacimiento": "1985-07-09", "tipoDocumento": "CC",
                     "numeroDocumento": "2077"}
                  ]
                }
                """.formatted(idExterno, ENTRADA.plusDays(desde), ENTRADA.plusDays(hasta));
    }

    private static String reservasDe(String canal) {
        return "/api/canales-externos/" + canal + "/reservas";
    }

    /** El mensaje de un canal externo, con la clave de Booking de las pruebas (DEC-73). */
    private static MockHttpServletRequestBuilder conClave(MockHttpServletRequestBuilder peticion) {
        return peticion.header(AutenticadorCanal.ENCABEZADO, CLAVE_BOOKING);
    }

    @Test
    @Tag("CAN-01")
    @Tag("CAN-05")
    void deberiaRegistrarListarYDesactivarCanales() throws Exception {
        String booking = canalExterno();
        enviar(post("/api/canales"), "{\"nombre\": \"Portal web\", \"tipo\": \"PORTAL\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.referenciaCredencial").value(nullValue()));

        mvc.perform(get("/api/canales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre").value(containsInAnyOrder("Booking", "Portal web")));
        mvc.perform(put("/api/canales/" + booking + "/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(put("/api/canales/" + booking + "/desactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put("/api/canales/CAN-99999/desactivar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("CAN-02")
    void unCanalMalFormadoEs400YUnExternoSinCredencialEs409() throws Exception {
        enviar(post("/api/canales"), "{\"nombre\": \" \", \"tipo\": \"WHATSAPP\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("nombre: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("tipo: debe ser PORTAL, DIRECTO o EXTERNO")));
        enviar(post("/api/canales"), "{\"nombre\": \"Expedia\", \"tipo\": \"EXTERNO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("CU-12")
    @Tag("RN-19")
    void elCanalCreaUnaReservaYAlRepetirlaRecibeLaMisma() throws Exception {
        String canal = canalExterno();

        String creada = enviar(conClave(post(reservasDe(canal))), pedido("BK-1", 0, 2))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reservas/RES-")))
                .andExpect(jsonPath("$.resultado").value("EXITOSO"))
                .andExpect(jsonPath("$.conflictoId").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(creada, "$.codigoReserva");

        enviar(conClave(post(reservasDe(canal))), pedido("BK-1", 0, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("DUPLICADO"))
                .andExpect(jsonPath("$.codigoReserva").value(codigo));
        mvc.perform(get("/api/reservas/" + codigo))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.canalOrigen").value("EXTERNO"))
                .andExpect(jsonPath("$.valor.total").value(200000));
    }

    @Test
    @Tag("RN-18")
    @Tag("CONF-04")
    void sinDisponibilidadRespondeConflictoYElAdministradorLoResuelve() throws Exception {
        String canal = canalExterno();
        reservaPendiente(ENTRADA); // ocupa el APT-101 dos noches

        String choque = enviar(conClave(post(reservasDe(canal))), pedido("BK-2", 1, 4))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultado").value("CONFLICTO"))
                .andExpect(jsonPath("$.codigoReserva").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        String conflicto = JsonPath.read(choque, "$.conflictoId");

        mvc.perform(get("/api/conflictos/pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(conflicto))
                .andExpect(jsonPath("$[0].idExterno").value("BK-2"))
                .andExpect(jsonPath("$[0].apartamento").value("APT-101"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
        enviar(put("/api/conflictos/" + conflicto + "/resolver"), "{\"decision\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("decision: es obligatoria")));
        enviar(put("/api/conflictos/" + conflicto + "/resolver"), "{\"decision\": \"Se reubicó en el APT-102\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.resueltoPor").value(SesionDePrueba.ADMINISTRADOR))
                .andExpect(jsonPath("$.decision").value("Se reubicó en el APT-102"));
        enviar(put("/api/conflictos/" + conflicto + "/resolver"), "{\"decision\": \"Otra\"}")
                .andExpect(status().isConflict());
        enviar(put("/api/conflictos/CON-99999/resolver"), "{\"decision\": \"Otra\"}")
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/conflictos/pendientes")).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Tag("DEC-62")
    @Tag("CAN-05")
    void unPedidoRechazadoEs409DeReglaYUnoMalFormado400() throws Exception {
        String canal = canalExterno();
        String sinElTitular = pedido("BK-3", 0, 2).replace("\"tipoDocumento\": \"CC\",\n", "\n")
                .replace("\"numeroDocumento\": \"2077\"}\n", "\"nacionalidad\": \"Colombiana\"}\n");

        enviar(conClave(post(reservasDe(canal))), sinElTitular)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(conClave(post(reservasDe(canal))), "{\"apartamento\": \"APT-101\", \"ocupantes\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("idExterno: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular: es obligatorio")));
        enviar(conClave(post(reservasDe("CAN-99999"))), pedido("BK-3", 0, 2))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CANAL_NO_AUTENTICADO"));
        mvc.perform(put("/api/canales/" + canal + "/desactivar")).andExpect(status().isOk());
        enviar(conClave(post(reservasDe(canal))), pedido("BK-3", 0, 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("BIT-01")
    @Tag("BIT-03")
    void laBitacoraDelCanalSeConsultaPorDiasYPorPaginas() throws Exception {
        String canal = canalExterno();
        enviar(conClave(post(reservasDe(canal))), pedido("BK-1", 0, 2)).andExpect(status().isCreated());
        enviar(conClave(post(reservasDe(canal))), pedido("BK-1", 0, 2)).andExpect(status().isOk());
        String eventos = "/api/canales/" + canal + "/eventos";

        mvc.perform(get(eventos).param("desde", HOY.toString()).param("hasta", HOY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[*].resultado").value(containsInAnyOrder("EXITOSO", "DUPLICADO")))
                .andExpect(jsonPath("$.contenido[0].operacion").value("CREAR_RESERVA"))
                .andExpect(jsonPath("$.contenido[0].sentido").value("ENTRANTE"))
                .andExpect(jsonPath("$.contenido[0].cargaUtil").value(containsString("\"idExterno\":\"BK-1\"")));
        mvc.perform(get(eventos).param("desde", HOY.plusDays(1).toString()).param("hasta", HOY.plusDays(2).toString()))
                .andExpect(jsonPath("$.contenido").isEmpty());
        mvc.perform(get(eventos).param("desde", HOY.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("hasta: es obligatorio")));
        mvc.perform(get("/api/canales/CAN-99999/eventos").param("desde", HOY.toString())
                        .param("hasta", HOY.toString()))
                .andExpect(status().isNotFound());
    }

    private ResultActions disponibilidad(String canal, String clave, int ocupantes) throws Exception {
        return mvc.perform(get("/api/canales-externos/" + canal + "/disponibilidad")
                .header(AutenticadorCanal.ENCABEZADO, clave).param("entrada", ENTRADA.toString())
                .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", String.valueOf(ocupantes)));
    }

    @Test
    @Tag("CU-13")
    @Tag("CAN-03")
    @Tag("DEC-73")
    void elCanalConsultaLaDisponibilidadConSuClave() throws Exception {
        String canal = canalExterno();

        disponibilidad(canal, CLAVE_BOOKING, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(containsInAnyOrder("APT-101", "APT-102")));
        reservaPendiente(ENTRADA);
        disponibilidad(canal, CLAVE_BOOKING, 2).andExpect(jsonPath("$[*].codigo").value(containsInAnyOrder("APT-102")));
        disponibilidad(canal, CLAVE_BOOKING, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("ocupantes: debe ser al menos 1")));
        disponibilidad(canal, "otra-clave", 2)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CANAL_NO_AUTENTICADO"));
        mvc.perform(get("/api/canales/" + canal + "/eventos").param("desde", HOY.toString())
                        .param("hasta", HOY.toString()))
                .andExpect(jsonPath("$.contenido[*].operacion").value(hasItem("CONSULTAR_DISPONIBILIDAD")));
        mvc.perform(put("/api/canales/" + canal + "/desactivar")).andExpect(status().isOk());
        disponibilidad(canal, CLAVE_BOOKING, 2).andExpect(status().isConflict());
    }

    @Test
    @Tag("CU-16")
    @Tag("RN-12")
    @Tag("CAN-05")
    void elCanalCancelaLaReservaQueTrajoAunqueEsteDesactivado() throws Exception {
        String canal = canalExterno();
        enviar(conClave(post(reservasDe(canal))), pedido("BK-1", 0, 2)).andExpect(status().isCreated());
        mvc.perform(put("/api/canales/" + canal + "/desactivar")).andExpect(status().isOk());
        String cancelar = reservasDe(canal) + "/BK-1/cancelar";

        mvc.perform(put(cancelar)).andExpect(status().isUnauthorized());
        mvc.perform(conClave(put(cancelar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(conClave(put(cancelar)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(conClave(put(reservasDe(canal) + "/BK-NO/cancelar")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @Tag("CAN-03")
    @Tag("DEC-73")
    void sinLaClaveOConUnaErradaElCanalNoEntra() throws Exception {
        String canal = canalExterno();

        enviar(post(reservasDe(canal)), pedido("BK-9", 0, 2))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje")
                        .value("El canal no se pudo autenticar: falta la clave del canal o no es válida"));
        enviar(post(reservasDe(canal)).header(AutenticadorCanal.ENCABEZADO, "clave-de-prueba-bookinG"),
                pedido("BK-9", 0, 2))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/canales/" + canal + "/eventos").param("desde", HOY.toString())
                        .param("hasta", HOY.toString()))
                .andExpect(jsonPath("$.contenido").isEmpty());
    }
}
