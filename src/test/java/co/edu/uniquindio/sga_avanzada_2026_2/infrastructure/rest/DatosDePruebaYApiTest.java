package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El arranque deja los datos de prueba de la sección 12.3 y la API los consulta con usuarios reales. Base H2 propia
 * y los valores reales de {@code src/main/resources/application.properties}.
 */
@SpringBootTest
@TestPropertySource(locations = "file:src/main/resources/application.properties", properties = {
        "spring.datasource.url=jdbc:h2:mem:datosdeprueba;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/migration",
        "sga.seguridad.jwt.secreto=secreto-solo-para-las-pruebas-del-sga-0123456789",
        "sga.seguridad.administrador.correo=", "sga.seguridad.administrador.clave=", "sga.correo.modo=local",
        "sga.datos-demo=true", "sga.demo.clave=Demo12345", "sga.planificador.activo=false"})
class DatosDePruebaYApiTest {

    private static final String RESERVAS = "/api/reservas";
    private static final LocalDate ENTRADA = LocalDate.now().plusDays(60);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    private String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\": \"" + correo + "\", \"contrasena\": \"Demo12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    private ResultActions como(String correo, MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(correo));
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(peticion);
    }

    private ResultActions admin(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        return como("admin.demo@example.com", peticion, cuerpo);
    }

    private static String reserva(String apartamento, String documento, LocalDate entrada, int vehiculos, int mascotas) {
        return """
                {"apartamento": "%s", "entrada": "%s", "salida": "%s", "canalOrigen": "DIRECTO", "vehiculos": %d,
                 "mascotas": %d, "horaEstimadaLlegada": "16:00:00",
                 "titular": {"nombre": "Prueba", "tipoDocumento": "CC", "numeroDocumento": "%s",
                             "correo": "t%s@example.com", "telefono": "3001234567"},
                 "ocupantes": [{"nombre": "Prueba", "fechaNacimiento": "1990-01-01", "tipoDocumento": "CC",
                                "numeroDocumento": "%s", "nacionalidad": "CO"}]}
                """.formatted(apartamento, entrada, entrada.plusDays(2), vehiculos, mascotas, documento, documento,
                documento);
    }

    @Test
    void alArrancarQuedanLosDiezApartamentosYUnUsuarioPorRol() throws Exception {
        admin(get("/api/apartamentos"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(10))
                .andExpect(jsonPath("$.contenido", hasSize(10)));
        admin(get("/api/usuarios"), null)
                .andExpect(jsonPath("$.contenido[*].rol").value(hasItem("ADMINISTRADOR")))
                .andExpect(jsonPath("$.contenido[*].rol").value(hasItem("RECEPCIONISTA")))
                .andExpect(jsonPath("$.contenido[*].rol").value(hasItem("PERSONAL_SERVICIO")));
    }

    @Test
    void hayReservasEnTodosLosEstadosYPorLosTresCanales() throws Exception {
        for (String estado : List.of("PENDIENTE", "CONFIRMADA", "EN_CURSO", "FINALIZADA", "CANCELADA", "NO_SHOW")) {
            admin(get(RESERVAS).param("estado", estado), null)
                    .andExpect(jsonPath("$.totalElementos").value(greaterThanOrEqualTo(1)));
        }
        for (String canal : List.of("PORTAL", "DIRECTO", "EXTERNO")) {
            admin(get(RESERVAS).param("canal", canal), null)
                    .andExpect(jsonPath("$.totalElementos").value(greaterThanOrEqualTo(1)));
        }
    }

    @Test
    void hayUnBloqueoVigenteYUnApartamentoFueraDeServicio() throws Exception {
        admin(get("/api/apartamentos/APT-109"), null).andExpect(jsonPath("$.bloqueos", hasSize(1)));
        admin(get("/api/apartamentos/APT-110"), null)
                .andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"));
    }

    @Test
    @Tag("CU-25")
    void lasReservasVanDeLaMasRecienteALaMasAntigua() throws Exception {
        String cuerpo = admin(get(RESERVAS), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> creadas = JsonPath.read(cuerpo, "$.contenido[*].creadaEn");
        assertEquals(creadas.stream().sorted(java.util.Comparator.reverseOrder()).toList(), creadas);
    }

    @Test
    @Tag("CU-07")
    void laBusquedaDevuelveTarjetasPaginadasYAplicaLosFiltros() throws Exception {
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[0].imagenPrincipal").isNotEmpty())
                .andExpect(jsonPath("$.contenido[0].valorEstimado").isNumber())
                .andExpect(jsonPath("$.contenido[0].ciudad").isNotEmpty());
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "2")
                        .param("capacidadMinima", "6"))
                .andExpect(jsonPath("$.contenido[*].capacidad").value(everyItem(greaterThanOrEqualTo(6))));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "2")
                        .param("precioMaximo", "1"))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "2")
                        .param("caracteristicas", "Chimenea", "NoExiste"))
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    @Tag("RN-06")
    void conFechasDeNacimientoElMenorNoPaga() throws Exception {
        String adultos = mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(1).toString()).param("ocupantes", "2")
                        .param("capacidadMinima", "6"))
                .andReturn().getResponse().getContentAsString();
        String conNino = mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(1).toString()).param("ocupantes", "2")
                        .param("capacidadMinima", "6")
                        .param("fechasNacimiento", "1990-01-01", ENTRADA.minusYears(3).toString()))
                .andReturn().getResponse().getContentAsString();

        double conDos = ((Number) JsonPath.read(adultos, "$.contenido[0].valorEstimado")).doubleValue();
        double conUno = ((Number) JsonPath.read(conNino, "$.contenido[0].valorEstimado")).doubleValue();
        assertEquals(conDos / 2, conUno);
    }

    @Test
    @Tag("CU-08")
    void elCalendarioEsPublicoYMarcaLasNochesOcupadas() throws Exception {
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/api/apartamentos/APT-103/calendario").param("desde", hoy.toString())
                        .param("hasta", hoy.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].libre").value(false));
        mvc.perform(get("/api/apartamentos/APT-103/calendario").param("desde", hoy.toString())
                        .param("hasta", hoy.plusYears(2).toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @Tag("CU-24")
    void laOcupacionMuestraReservasYBloqueos() throws Exception {
        LocalDate hoy = LocalDate.now();
        como("recepcion.demo@example.com", get("/api/ocupacion").param("desde", hoy.minusDays(2).toString())
                .param("hasta", hoy.plusDays(30).toString()).param("apartamento", "APT-109"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].bloqueos", hasSize(1)));
        como("recepcion.demo@example.com", get("/api/ocupacion").param("desde", hoy.toString())
                .param("hasta", hoy.plusDays(1).toString()).param("apartamento", "APT-103"), null)
                .andExpect(jsonPath("$.contenido[0].reservas[0].estado").value("EN_CURSO"));
        mvc.perform(get("/api/ocupacion").param("desde", hoy.toString()).param("hasta", hoy.plusDays(1).toString()))
                .andExpect(status().isUnauthorized());
        como("servicio.demo@example.com", get("/api/ocupacion").param("desde", hoy.toString())
                .param("hasta", hoy.plusDays(1).toString()), null)
                .andExpect(status().isForbidden());
    }

    @Test
    @Tag("CU-41")
    void losPendientesDePreparacionSonDelPersonal() throws Exception {
        admin(put("/api/apartamentos/APT-108/iniciar-preparacion"), null).andExpect(status().isConflict());
        como("servicio.demo@example.com", get("/api/apartamentos/pendientes-preparacion"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10));
        mvc.perform(get("/api/apartamentos/pendientes-preparacion")).andExpect(status().isUnauthorized());
        como("recepcion.demo@example.com", get("/api/apartamentos/pendientes-preparacion"), null)
                .andExpect(status().isForbidden());
    }

    @Test
    @Tag("CU-31")
    void elAdministradorEditaElApartamentoYSusImagenes() throws Exception {
        String imagen = "{\"url\": \"https://cdn.sga.co/nueva-108.jpg\"}";

        admin(put("/api/apartamentos/APT-108"), """
                {"nombre": "Bahareque renovado", "descripcion": "Nuevo", "dormitorios": 1, "caracteristicas": ["Wifi"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bahareque renovado"));
        admin(put("/api/apartamentos/APT-108"), "{\"nombre\": \"\", \"dormitorios\": 0}")
                .andExpect(status().isBadRequest());
        admin(post("/api/apartamentos/APT-108/imagenes"), imagen)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/apartamentos/APT-108"));
        admin(post("/api/apartamentos/APT-108/imagenes"), imagen).andExpect(status().isConflict());
        admin(put("/api/apartamentos/APT-108/imagenes/principal"), imagen).andExpect(status().isOk());
        admin(put("/api/apartamentos/APT-108/imagenes/quitar"), imagen).andExpect(status().isOk());
        admin(put("/api/apartamentos/APT-108/imagenes/quitar"), imagen).andExpect(status().isConflict());
        como("recepcion.demo@example.com", put("/api/apartamentos/APT-108"), """
                {"nombre": "No", "dormitorios": 1, "caracteristicas": ["Wifi"]}
                """).andExpect(status().isForbidden());
    }

    @Test
    @Tag("CU-30")
    void elAdministradorEditaLosDatosDelAlojamiento() throws Exception {
        admin(put("/api/alojamiento/datos"), """
                {"nombre": "Puerta al Sol", "descripcion": "Apartamentos", "ciudad": "Salento",
                 "direccion": "Calle Real 1", "normas": "Silencio desde las 22:00"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciudad").value("Salento"))
                .andExpect(jsonPath("$.normas").value("Silencio desde las 22:00"));
        admin(put("/api/alojamiento/datos"), "{\"nombre\": \"\"}").andExpect(status().isBadRequest());
    }

    @Test
    @Tag("TAR-05")
    void elHistorialDeTarifasConservaLasVersiones() throws Exception {
        String temporada = "{\"valorPorOcupante\": 999000}";
        String cuerpo = admin(get("/api/temporadas"), null).andReturn().getResponse().getContentAsString();
        String base = JsonPath.read(cuerpo, "$.temporadas[0].id");

        admin(put("/api/apartamentos/APT-106/tarifas/" + base), temporada).andExpect(status().isOk());

        admin(get("/api/apartamentos/APT-106/tarifas/historial"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].version").value(hasItem(2)))
                .andExpect(jsonPath("$.contenido[*].version").value(hasItem(1)));
    }

    @Test
    @Tag("POL-04")
    void lasVersionesDeLaPoliticaSeConsultan() throws Exception {
        admin(get("/api/politicas"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamano").value(10))
                .andExpect(jsonPath("$.contenido[0].version").value(1));
    }

    @Test
    @Tag("RP-02")
    void elParqueaderoRechazaLoQueNoCabe() throws Exception {
        LocalDate fecha = ENTRADA.plusDays(100);
        admin(post("/api/reservas"), reserva("APT-101", "70001", fecha, 8, 0))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehiculos").value(8));
        admin(post("/api/reservas"), reserva("APT-102", "70002", fecha, 1, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("parqueadero")));
        admin(post("/api/reservas"), reserva("APT-102", "70003", fecha, 0, 0)).andExpect(status().isCreated());
    }

    @Test
    @Tag("RP-03")
    void lasMascotasTienenLimiteYGeneranCargo() throws Exception {
        LocalDate fecha = ENTRADA.plusDays(120);
        admin(post("/api/reservas"), reserva("APT-105", "71001", fecha, 0, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("mascotas")));
        String cuerpo = admin(post("/api/reservas"), reserva("APT-105", "71002", fecha, 0, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mascotas").value(2))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(cuerpo, "$.codigo");

        admin(get("/api/reservas/" + codigo + "/folio"), null)
                .andExpect(jsonPath("$.cargos[*].tipo").value(hasItem("SERVICIO_ADICIONAL")))
                .andExpect(jsonPath("$.cargos[*].valor").value(hasItem(60000)));
    }

    @Test
    void losErroresSiempreLleganComoErrorResponse() throws Exception {
        admin(get("/api/no-existe"), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        admin(put("/api/reservas"), null)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
    }
}
