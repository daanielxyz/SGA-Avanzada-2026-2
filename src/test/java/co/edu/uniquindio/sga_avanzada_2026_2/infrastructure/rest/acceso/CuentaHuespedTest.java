package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.NotificadorLocal;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.NotificadorLocal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.NotificadorLocal.Mensaje;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La cuenta del huésped de extremo a extremo (B3b · DEC-68 · DEC-69): la reserva la crea recepción, como lo haría
 * con el administrador de las demás pruebas, y el huésped entra con su correo, su documento y el PIN que le deja el
 * {@link NotificadorLocal}. Sus peticiones van por un {@code MockMvc} sin la sesión de prueba, como las de un cliente.
 */
class CuentaHuespedTest extends PruebaRestDeReservas {

    private static final String ANA = "ana@correo.co";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private NotificadorLocal notificador;

    private MockMvc cliente;

    @BeforeEach
    void prepararCliente() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private ResultActions pedir(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return cliente.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions solicitarPin(String correo, String documento) throws Exception {
        return pedir(post("/api/auth/huesped/pin"),
                "{\"correo\": \"%s\", \"numeroDocumento\": \"%s\"}".formatted(correo, documento));
    }

    private ResultActions entrar(String correo, String documento, String pin) throws Exception {
        return pedir(post("/api/auth/huesped/login"),
                "{\"correo\": \"%s\", \"numeroDocumento\": \"%s\", \"pin\": \"%s\"}".formatted(correo, documento, pin));
    }

    /** Pide el PIN, lo toma del correo y entra; devuelve el token. */
    private String entrarComoHuesped(String correo, String documento) throws Exception {
        solicitarPin(correo, documento).andExpect(status().isAccepted());
        String respuesta = entrar(correo, documento, ultimoPin(correo)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private List<Mensaje> mensajesConPin(String destino) {
        return notificador.enviados().stream()
                .filter(m -> m.destino().equals(destino) && m.cuerpo().contains("PIN"))
                .toList();
    }

    private String ultimoPin(String destino) {
        List<Mensaje> conPin = mensajesConPin(destino);
        Matcher pin = Pattern.compile("\\b(\\d{6})\\b").matcher(conPin.getLast().cuerpo());
        assertTrue(pin.find(), "el mensaje no trae el PIN");
        return pin.group(1);
    }

    private static MockHttpServletRequestBuilder con(String token, MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String titularDe(String codigo) throws Exception {
        String reserva = mvc.perform(get("/api/reservas/" + codigo)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(reserva, "$.titularId");
    }

    /** Reserva de Eva (CC 2077) en el APT-102: otro titular, con otra cuenta. */
    private String reservaDeEva() throws Exception {
        String json = reservaJson("APT-102", HOY.plusDays(5), HOY.plusDays(6)).replace("1094", "2077")
                .replace(ANA, "eva@correo.co").replace("Ana Ríos", "Eva Mora");
        String respuesta = enviar(post("/api/reservas"), json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.codigo");
    }

    @Test
    @Tag("DEC-68")
    @Tag("USU-04")
    @Tag("ROL-04")
    void elHuespedEntraConPinYSoloVeYCancelaLoSuyo() throws Exception {
        String deAna = reservaPendiente(HOY.plusDays(5));
        String deEva = reservaDeEva();

        String token = entrarComoHuesped(ANA, "1094");

        cliente.perform(con(token, get("/api/mis-reservas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].codigo").value(deAna));
        cliente.perform(con(token, get("/api/reservas/" + deAna))).andExpect(status().isOk());
        cliente.perform(con(token, get("/api/reservas/" + deAna + "/folio"))).andExpect(status().isOk());
        cliente.perform(con(token, get("/api/reservas/" + deEva)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        cliente.perform(con(token, get("/api/reservas/" + deEva + "/folio"))).andExpect(status().isForbidden());
        cliente.perform(con(token, put("/api/reservas/" + deEva + "/cancelar"))).andExpect(status().isForbidden());
        cliente.perform(con(token, put("/api/reservas/" + deAna + "/cancelar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        cliente.perform(con(token, get("/api/canales"))).andExpect(status().isForbidden());
        cliente.perform(con(token, put("/api/reservas/" + deEva + "/confirmar"))).andExpect(status().isForbidden());
    }

    @Test
    @Tag("DEC-68")
    void elTokenDelHuespedLlevaSuTitularYSuRol() throws Exception {
        String titular = titularDe(reservaPendiente(HOY.plusDays(5)));
        solicitarPin(ANA, "1094")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje", containsString("le enviamos un PIN al correo")));

        entrar(ANA, "1094", ultimoPin(ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value(titular))
                .andExpect(jsonPath("$.nombre").value("Ana Ríos"))
                .andExpect(jsonPath("$.rol").value("HUESPED"));
    }

    @Test
    @Tag("DEC-68")
    void unPinEquivocadoUsadoOConCincoFallosNoAbreLaCuenta() throws Exception {
        reservaPendiente(HOY.plusDays(5));
        solicitarPin(ANA, "1094");
        String pin = ultimoPin(ANA);
        String equivocado = pin.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            entrar(ANA, "1094", equivocado)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }
        entrar(ANA, "1094", pin).andExpect(status().isUnauthorized());

        solicitarPin(ANA, "1094");
        String nuevo = ultimoPin(ANA);
        entrar(ANA, "1094", nuevo).andExpect(status().isOk());
        entrar(ANA, "1094", nuevo).andExpect(status().isUnauthorized());
        entrar(ANA, "1094", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("pin: debe tener 6 dígitos")));
    }

    @Test
    @Tag("DEC-68")
    @Tag("USU-08")
    void sinCuentaLaRespuestaEsLaMismaYNoSaleNingunCorreo() throws Exception {
        reservaPendiente(HOY.plusDays(5));
        int antes = notificador.enviados().size();

        String conCuenta = solicitarPin(ANA, "1094").andReturn().getResponse().getContentAsString();
        int conUnPin = notificador.enviados().size();
        String sinCuenta = solicitarPin("nadie@correo.co", "1094").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        solicitarPin(ANA, "9999").andExpect(status().isAccepted());

        assertEquals(conCuenta, sinCuenta);
        assertEquals(antes + 1, conUnPin);
        assertEquals(conUnPin, notificador.enviados().size());
        entrar("nadie@correo.co", "1094", "123456")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Los datos o el PIN no son válidos, o el PIN ya venció"));
    }

    @Test
    @Tag("DEC-68")
    void noSeEnvianMasDeTresPinCadaQuinceMinutos() throws Exception {
        reservaPendiente(HOY.plusDays(5));
        int antes = mensajesConPin(ANA).size(); // el notificador lo comparten todas las pruebas

        for (int i = 0; i < 4; i++) {
            solicitarPin(ANA, "1094").andExpect(status().isAccepted());
        }

        assertEquals(antes + 3, mensajesConPin(ANA).size());
    }

    @Test
    @Tag("DEC-69")
    void elCorreoSeCambiaConElPinQueLlegaAlTelefonoYAlCorreoActual() throws Exception {
        String titular = titularDe(reservaPendiente(HOY.plusDays(5)));
        String nuevo = "ana.nueva@correo.co";

        pedir(post("/api/auth/huesped/cambio-correo"),
                "{\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1094\", \"nuevoCorreo\": \"%s\"}".formatted(nuevo))
                .andExpect(status().isAccepted());
        Mensaje sms = notificador.enviados().stream().filter(m -> m.canal() == Canal.SMS).toList().getLast();
        assertEquals("300 123 4567", sms.destino());
        assertTrue(sms.cuerpo().contains("a***@correo.co"));
        String pin = ultimoPin(ANA);
        assertTrue(sms.cuerpo().contains(pin));

        pedir(put("/api/auth/huesped/cambio-correo"),
                "{\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1094\", \"pin\": \"%s\"}".formatted(pin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/titulares/" + titular)).andExpect(jsonPath("$.correo").value(nuevo));
        List<Mensaje> enviados = notificador.enviados();
        assertTrue(enviados.stream().anyMatch(m -> m.destino().equals(ANA)
                && m.asunto().equals("El correo de tu cuenta cambió")));
        assertTrue(enviados.stream().anyMatch(m -> m.destino().equals(nuevo)
                && m.asunto().equals("Este es ahora el correo de tu cuenta")));
        entrarComoHuesped(nuevo, "1094");
        pedir(put("/api/auth/huesped/cambio-correo"),
                "{\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1094\", \"pin\": \"%s\"}".formatted(pin))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("DEC-69")
    void unaReservaConOtroCorreoNoCambiaElDeLaCuentaYLoAvisa() throws Exception {
        String titular = titularDe(reservaPendiente(HOY.plusDays(5)));
        String otroCorreo = reservaJson("APT-102", HOY.plusDays(10), HOY.plusDays(11))
                .replace(ANA, "otro@correo.co").replace("300 123 4567", "311 999 9999");

        enviar(post("/api/reservas"), otroCorreo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titularId").value(titular))
                .andExpect(jsonPath("$.avisoCuenta", containsString("a***@correo.co")));

        mvc.perform(get("/api/titulares/" + titular))
                .andExpect(jsonPath("$.correo").value(ANA))
                .andExpect(jsonPath("$.telefono").value("300 123 4567"));
        mvc.perform(get("/api/reservas/" + reservaPendiente(HOY.plusDays(20))))
                .andExpect(jsonPath("$.avisoCuenta").value(nullValue()));
    }

    @Test
    @Tag("DEC-68")
    @Tag("DEC-69")
    void enSuCuentaVeSusDatosYCambiaNombreYTelefono() throws Exception {
        reservaPendiente(HOY.plusDays(5));
        String token = entrarComoHuesped(ANA, "1094");

        cliente.perform(con(token, get("/api/mi-cuenta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(ANA))
                .andExpect(jsonPath("$.numeroDocumento").value("1094"));
        cliente.perform(con(token, put("/api/mi-cuenta")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana María Ríos\", \"telefono\": \"311 222 3333\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María Ríos"))
                .andExpect(jsonPath("$.telefono").value("311 222 3333"))
                .andExpect(jsonPath("$.correo").value(ANA));
        cliente.perform(con(token, put("/api/mi-cuenta")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("telefono: es obligatorio")));
        mvc.perform(get("/api/mi-cuenta")).andExpect(status().isForbidden());
    }

    @Test
    @Tag("DEC-68")
    void recepcionDebeDarCorreoYTelefonoDelTitular() throws Exception {
        String sinContacto = reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7))
                .replace(", \"telefono\": \"300 123 4567\"", "")
                .replace("\"correo\": \"ana@correo.co\"", "\"correo\": \"\"");

        enviar(post("/api/reservas"), sinContacto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.correo: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.telefono: es obligatorio")));
    }
}
