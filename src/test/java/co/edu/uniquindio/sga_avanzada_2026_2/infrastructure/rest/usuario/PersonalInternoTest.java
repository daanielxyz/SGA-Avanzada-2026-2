package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.NotificadorLocal;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.NotificadorLocal.Mensaje;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Usuario;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El personal interno de extremo a extremo (B3d · DEC-71): alta con código de activación, recuperación de la
 * contraseña, perfil, cierre de sesión y cortes de sesión, con tokens reales. Marta (USR-950) es la administradora
 * guardada en la base; el {@code MockMvc} heredado entra como el administrador de prueba, y {@code cliente} sin sesión.
 */
class PersonalInternoTest extends PruebaRestDeReservas {

    private static final String CLAVE = "PuertaAlSol2026";
    private static final String RITA = "rita@puertaalsol.co";
    private static final String NUEVA = """
            {"nombre": "Rita Gómez", "correo": "rita@puertaalsol.co", "telefono": "300 555 1234",
             "tipoDocumento": "CC", "numeroDocumento": "4321", "fechaNacimiento": "1992-04-10",
             "rol": "RECEPCIONISTA"}
            """;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder cifrador;

    @Autowired
    private NotificadorLocal notificador;

    private MockMvc cliente;

    @BeforeEach
    void prepararPersonal() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        usuarios.saveAndFlush(Usuario.crear(new UsuarioId("USR-950"), "ALO-1", "Marta Díaz",
                new Correo("marta@puertaalsol.co"), cifrador.encode(CLAVE), Rol.ADMINISTRADOR));
    }

    private ResultActions pedir(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return cliente.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions entrar(String correo, String clave) throws Exception {
        return pedir(post("/api/auth/login"), "{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(correo, clave));
    }

    private String tokenDe(String correo, String clave) throws Exception {
        String respuesta = entrar(correo, clave).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private static MockHttpServletRequestBuilder con(String token, MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String ultimoCodigo(String destino) {
        List<Mensaje> conCodigo = notificador.enviados().stream()
                .filter(m -> m.destino().equals(destino) && m.cuerpo().contains("código"))
                .toList();
        Matcher codigo = Pattern.compile("\\b(\\d{6})\\b").matcher(conCodigo.getLast().cuerpo());
        assertTrue(codigo.find(), "el correo no trae el código");
        return codigo.group(1);
    }

    private static String fijar(String correo, String codigo, String clave) {
        return "{\"correo\": \"%s\", \"codigo\": \"%s\", \"contrasena\": \"%s\"}".formatted(correo, codigo, clave);
    }

    /** Rita, recepcionista, creada por el administrador y activada; devuelve su id. */
    private String ritaActivada() throws Exception {
        String respuesta = enviar(post("/api/usuarios"), NUEVA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        pedir(post("/api/auth/activar"), fijar(RITA, ultimoCodigo(RITA), CLAVE)).andExpect(status().isOk());
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @Tag("CU-06")
    @Tag("USU-03")
    @Tag("DEC-71")
    void elAdministradorCreaAlUsuarioYEsteFijaSuContrasenaConElCodigo() throws Exception {
        enviar(post("/api/usuarios"), NUEVA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.activado").value(false))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        entrar(RITA, CLAVE).andExpect(status().isUnauthorized());

        pedir(post("/api/auth/activar"), fijar(RITA, ultimoCodigo(RITA), "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles")
                        .value(hasItem("contrasena: debe tener mínimo 8 caracteres, con mayúsculas y números")));
        pedir(post("/api/auth/activar"), fijar(RITA, ultimoCodigo(RITA), CLAVE))
                .andExpect(status().isOk());

        entrar(RITA, CLAVE).andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("RECEPCIONISTA"));
        mvc.perform(get("/api/usuarios"))
                .andExpect(jsonPath("$.contenido[*].correo").value(hasItem(RITA)));
    }

    @Test
    @Tag("USU-01")
    @Tag("USU-03")
    void unCorreoRepetidoUnHuespedODatosIncompletosNoCreanUsuario() throws Exception {
        enviar(post("/api/usuarios"), NUEVA.replace(RITA, "marta@puertaalsol.co"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"));
        enviar(post("/api/usuarios"), NUEVA.replace("RECEPCIONISTA", "HUESPED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles")
                        .value(hasItem("rol: debe ser RECEPCIONISTA, PERSONAL_SERVICIO o ADMINISTRADOR")));
        enviar(post("/api/usuarios"), "{\"correo\": \"x@puertaalsol.co\", \"rol\": \"RECEPCIONISTA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("telefono: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("fechaNacimiento: es obligatoria")));
        cliente.perform(con(tokenDe("marta@puertaalsol.co", CLAVE), get("/api/usuarios"))).andExpect(status().isOk());
    }

    @Test
    @Tag("CU-05")
    @Tag("USU-06")
    @Tag("USU-08")
    void conElCodigoDeRecuperacionSeFijaOtraYLasSesionesAnterioresSeCortan() throws Exception {
        String antes = tokenDe("marta@puertaalsol.co", CLAVE);
        String respuestaSinCuenta = pedir(post("/api/auth/recuperar-contrasena"), "{\"correo\": \"nadie@x.co\"}")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        String respuesta = pedir(post("/api/auth/recuperar-contrasena"), "{\"correo\": \"marta@puertaalsol.co\"}")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String codigo = ultimoCodigo("marta@puertaalsol.co");
        pedir(post("/api/auth/restablecer-contrasena"), fijar("marta@puertaalsol.co", "000000", "OtraClave2026"))
                .andExpect(status().isUnauthorized());
        pedir(post("/api/auth/restablecer-contrasena"), fijar("marta@puertaalsol.co", codigo, "OtraClave2026"))
                .andExpect(status().isOk());

        assertEquals(respuestaSinCuenta, respuesta);
        entrar("marta@puertaalsol.co", CLAVE).andExpect(status().isUnauthorized());
        entrar("marta@puertaalsol.co", "OtraClave2026").andExpect(status().isOk());
        cliente.perform(con(antes, get("/api/usuarios")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        pedir(post("/api/auth/restablecer-contrasena"), fijar("marta@puertaalsol.co", codigo, "Tercera2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("CU-48")
    void cerrarSesionDejaSinValorEseTokenYNingunOtro() throws Exception {
        String uno = tokenDe("marta@puertaalsol.co", CLAVE);
        String otro = tokenDe("marta@puertaalsol.co", CLAVE);

        cliente.perform(con(uno, post("/api/auth/logout"))).andExpect(status().isOk());

        cliente.perform(con(uno, get("/api/usuarios"))).andExpect(status().isUnauthorized());
        cliente.perform(con(otro, get("/api/usuarios"))).andExpect(status().isOk());
        cliente.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("ROL-03")
    @Tag("USU-05")
    @Tag("DEC-71")
    void cambiarElRolOEliminarCortaLasSesionesAlInstante() throws Exception {
        String rita = ritaActivada();
        String token = tokenDe(RITA, CLAVE);
        cliente.perform(con(token, get("/api/titulares/TIT-99999"))).andExpect(status().isNotFound());

        enviar(put("/api/usuarios/" + rita), NUEVA.replace("\"correo\": \"rita@puertaalsol.co\", ", "")
                .replace("RECEPCIONISTA", "PERSONAL_SERVICIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PERSONAL_SERVICIO"))
                .andExpect(jsonPath("$.correo").value(RITA));
        cliente.perform(con(token, get("/api/titulares/TIT-99999"))).andExpect(status().isUnauthorized());

        String deServicio = tokenDe(RITA, CLAVE);
        mvc.perform(put("/api/usuarios/" + rita + "/eliminar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminado").value(true));
        cliente.perform(con(deServicio, get("/api/apartamentos/APT-101/novedades")))
                .andExpect(status().isUnauthorized());
        entrar(RITA, CLAVE).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/usuarios/" + rita + "/eliminar")).andExpect(status().isConflict());
        mvc.perform(put("/api/usuarios/USR-99999/eliminar")).andExpect(status().isNotFound());
    }

    @Test
    @Tag("DEC-71")
    void alUltimoAdministradorNoSeLeQuitaElRolNiSeElimina() throws Exception {
        String comoRecepcionista = NUEVA.replace("\"correo\": \"rita@puertaalsol.co\", ", "")
                .replace("Rita Gómez", "Marta Díaz");

        mvc.perform(put("/api/usuarios/USR-950/eliminar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put("/api/usuarios/USR-950"), comoRecepcionista).andExpect(status().isConflict());

        String rita = ritaActivada();
        enviar(put("/api/usuarios/" + rita), comoRecepcionista.replace("RECEPCIONISTA", "ADMINISTRADOR")
                .replace("Marta Díaz", "Rita Gómez")).andExpect(status().isOk());
        mvc.perform(put("/api/usuarios/USR-950/eliminar")).andExpect(status().isOk());
    }

    @Test
    @Tag("CU-03")
    @Tag("CU-04")
    @Tag("USU-02")
    void cadaUnoCambiaSuPerfilYSuContrasena() throws Exception {
        ritaActivada();
        String token = tokenDe(RITA, CLAVE);
        String perfil = """
                {"nombre": "Rita María Gómez", "telefono": "311 222 3333", "tipoDocumento": "CC",
                 "numeroDocumento": "4321", "fechaNacimiento": "1992-04-10"}
                """;

        cliente.perform(con(token, get("/api/usuarios/perfil")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(RITA));
        cliente.perform(con(token, put("/api/usuarios/perfil")).contentType(MediaType.APPLICATION_JSON).content(perfil))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rita María Gómez"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"));
        cliente.perform(con(token, put("/api/usuarios/perfil/contrasena")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\": \"Equivocada1\", \"nueva\": \"OtraClave2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        cliente.perform(con(token, put("/api/usuarios/perfil/contrasena")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\": \"" + CLAVE + "\", \"nueva\": \"OtraClave2026\"}"))
                .andExpect(status().isOk());

        cliente.perform(con(token, get("/api/usuarios/perfil"))).andExpect(status().isUnauthorized());
        entrar(RITA, "OtraClave2026").andExpect(status().isOk());
        cliente.perform(con(tokenDe(RITA, "OtraClave2026"), get("/api/usuarios"))).andExpect(status().isForbidden());
    }
}
