package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.EmisorTokens;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SeguridadPropiedades;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Usuario;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Duration;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inicio de sesión y uso del token real (B3a · DEC-67). A diferencia de las demás pruebas REST, aquí el
 * {@code MockMvc} no trae la sesión de prueba: cada petición entra como entraría un cliente, con el token que le dio
 * {@code POST /api/auth/login} o sin ninguno.
 */
class SesionControllerTest extends PruebaRestDeReservas {

    private static final String CLAVE = "Recepcion2026";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder cifrador;

    @Autowired
    private JwtEncoder codificador;

    @Autowired
    private Clock reloj;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private MockMvc cliente;

    @BeforeEach
    void prepararUsuarios() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        guardar("USR-901", "Rita Gómez", "rita@puertaalsol.co", Rol.RECEPCIONISTA);
        guardar("USR-902", "Saúl Pérez", "saul@puertaalsol.co", Rol.PERSONAL_SERVICIO);
    }

    private Usuario guardar(String id, String nombre, String correo, Rol rol) {
        return usuarios.saveAndFlush(Usuario.crear(new UsuarioId(id), "ALO-1", nombre, new Correo(correo),
                cifrador.encode(CLAVE), rol));
    }

    private ResultActions entrar(String correo, String contrasena) throws Exception {
        return cliente.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"contrasena\": \"%s\"}".formatted(correo, contrasena)));
    }

    private String tokenDe(String correo) throws Exception {
        String respuesta = entrar(correo, CLAVE).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private static MockHttpServletRequestBuilder con(String token, MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @Test
    @Tag("CU-02")
    @Tag("ROL-02")
    void deberiaEntrarYRecibirUnTokenConSuRol() throws Exception {
        entrar("Rita@PuertaAlSol.co", CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario").value("USR-901"))
                .andExpect(jsonPath("$.nombre").value("Rita Gómez"))
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.expiraEn").exists())
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    @Tag("CU-02")
    @Tag("USU-05")
    void unaCredencialInvalidaEs401SinDecirCualDatoFallo() throws Exception {
        String mensaje = "Correo o contraseña incorrectos";

        entrar("rita@puertaalsol.co", "Otra2026Clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.mensaje").value(mensaje));
        entrar("nadie@puertaalsol.co", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(mensaje));
        jdbc.update("update usuario set eliminado = true where id = 'USR-902'");
        em.clear();
        entrar("saul@puertaalsol.co", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(mensaje));
    }

    @Test
    void unInicioDeSesionMalFormadoEs400() throws Exception {
        cliente.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"rita\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("correo: no tiene formato de correo")))
                .andExpect(jsonPath("$.detalles").value(hasItem("contrasena: es obligatoria")));
    }

    @Test
    @Tag("CU-02")
    @Tag("DEC-22")
    void elTokenAbreLoQuePermiteSuRolYNadaMas() throws Exception {
        String recepcion = tokenDe("rita@puertaalsol.co");

        cliente.perform(con(recepcion, get("/api/titulares/TIT-99999")))
                .andExpect(status().isNotFound());
        cliente.perform(con(recepcion, get("/api/canales")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    @Tag("DEC-61")
    @Tag("NOV-05")
    void elAutorEsElUsuarioDelToken() throws Exception {
        String servicio = tokenDe("saul@puertaalsol.co");

        cliente.perform(con(servicio, post("/api/apartamentos/APT-101/novedades"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\": \"Grifo del baño gotea\", \"gravedad\": \"MEDIA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.historial[0].autor").value("USR-902"));
    }

    @Test
    @Tag("CU-02")
    void sinTokenOConUnoQueNoValeEs401() throws Exception {
        String vencido = new EmisorTokens(codificador, Clock.offset(reloj, Duration.ofHours(-3)),
                new SeguridadPropiedades(new SeguridadPropiedades.Jwt(null, 60), null))
                .emitir(usuarios.findByCorreo("rita@puertaalsol.co").orElseThrow()).token();
        String alterado = tokenDe("rita@puertaalsol.co").replaceFirst(".$", "x") + "y";

        cliente.perform(get("/api/canales"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        cliente.perform(con(vencido, get("/api/reservas/RES-2026-00001")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        cliente.perform(con(alterado, get("/api/reservas/RES-2026-00001")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @Tag("CU-07")
    @Tag("CU-50")
    void loPublicoSeConsultaSinToken() throws Exception {
        cliente.perform(get("/api/alojamiento")).andExpect(status().isOk());
        cliente.perform(get("/api/apartamentos")).andExpect(status().isOk());
        cliente.perform(get("/api/apartamentos/APT-101")).andExpect(status().isOk());
        cliente.perform(get("/api/apartamentos/disponibles").param("entrada", HOY.plusDays(5).toString())
                        .param("salida", HOY.plusDays(7).toString()).param("ocupantes", "2"))
                .andExpect(status().isOk());
    }
}
