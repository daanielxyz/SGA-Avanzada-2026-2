package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Buscar y cotizar desde el portal, sin sesión, como un visitante (CU-07 · CU-08 · CU-09), sobre el APT-101
 * (capacidad 4) y el APT-102 (capacidad 2) de {@link PruebaRestDeReservas}, a 100.000 por ocupante y noche.
 */
class ApartamentoPortalControllerTest extends PruebaRestDeReservas {

    private static final LocalDate ENTRADA = HOY.plusDays(5);
    private static final LocalDate SALIDA = HOY.plusDays(7);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc visitante;

    @BeforeEach
    void prepararVisitante() {
        visitante = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private ResultActions buscar(LocalDate entrada, LocalDate salida, String ocupantes) throws Exception {
        return visitante.perform(get("/api/portal/apartamentos/disponibles").param("entrada", entrada.toString())
                .param("salida", salida.toString()).param("ocupantes", ocupantes));
    }

    @Test
    @Tag("CU-07")
    @Tag("DEC-67")
    void elVisitanteVeLosLibresSoloConLosDatosPublicos() throws Exception {
        buscar(ENTRADA, SALIDA, "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(containsInAnyOrder("APT-101", "APT-102")))
                .andExpect(jsonPath("$[0].nombre").exists())
                .andExpect(jsonPath("$[0].capacidad").exists())
                .andExpect(jsonPath("$[0].dormitorios").value(2))
                .andExpect(jsonPath("$[0].imagenes[0].url").value("https://cdn.sga.co/apt.jpg"))
                .andExpect(jsonPath("$[0].imagenes[0].principal").value(true))
                .andExpect(jsonPath("$[0].caracteristicas").value(contains("Balcón")))
                .andExpect(jsonPath("$[0].estadoOperativo").doesNotExist())
                .andExpect(jsonPath("$[0].bloqueos").doesNotExist())
                .andExpect(jsonPath("$[0].activo").doesNotExist())
                .andExpect(jsonPath("$[0].alojamientoId").doesNotExist());
    }

    @Test
    @Tag("RN-02")
    void unGrupoQueNoCabeNoVeElApartamento() throws Exception {
        buscar(ENTRADA, SALIDA, "3").andExpect(jsonPath("$[*].codigo").value(contains("APT-101")));
        buscar(ENTRADA, SALIDA, "5").andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Tag("RN-01")
    void unApartamentoYaReservadoEsasNochesNoAparece() throws Exception {
        reservaPendiente(ENTRADA);

        buscar(ENTRADA, SALIDA, "2").andExpect(jsonPath("$[*].codigo").value(contains("APT-102")));
        buscar(SALIDA, SALIDA.plusDays(1), "2")
                .andExpect(jsonPath("$[*].codigo").value(containsInAnyOrder("APT-101", "APT-102")));
    }

    @Test
    @Tag("APA-16")
    void unApartamentoRetiradoNoExisteParaElPublico() throws Exception {
        mvc.perform(put("/api/apartamentos/APT-102/retirar")).andExpect(status().isOk());

        buscar(ENTRADA, SALIDA, "2").andExpect(jsonPath("$[*].codigo").value(contains("APT-101")));
        visitante.perform(get("/api/portal/apartamentos/APT-102"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @Tag("CU-08")
    void laFichaMuestraLoPublicoDeUnApartamento() throws Exception {
        visitante.perform(get("/api/portal/apartamentos/APT-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("APT-101"))
                .andExpect(jsonPath("$.capacidad").value(4))
                .andExpect(jsonPath("$.caracteristicas").value(hasItem("Balcón")))
                .andExpect(jsonPath("$.bloqueos").doesNotExist());
        visitante.perform(get("/api/portal/apartamentos/APT-999")).andExpect(status().isNotFound());
    }

    @Test
    @Tag("RN-03")
    void unaBusquedaMalHechaDiceQueFalla() throws Exception {
        buscar(ENTRADA, SALIDA, "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        buscar(SALIDA, ENTRADA, "2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("CU-09")
    @Tag("RN-06")
    void cadaLibreTraeSuPrecioYLosNinosNoPagan() throws Exception {
        visitante.perform(get("/api/portal/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", SALIDA.toString()).param("ocupantes", "3").param("ninos", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("APT-101"))
                .andExpect(jsonPath("$[0].precio.desglose.length()").value(2))
                .andExpect(jsonPath("$[0].precio.desglose[0].noche").value(ENTRADA.toString()))
                .andExpect(jsonPath("$[0].precio.desglose[0].tarifa").value(100000))
                .andExpect(jsonPath("$[0].precio.desglose[0].ocupantesFacturables").value(2))
                .andExpect(jsonPath("$[0].precio.desglose[0].temporada").doesNotExist())
                .andExpect(jsonPath("$[0].precio.total").value(400000));
    }

    @Test
    @Tag("RN-06")
    void sinDecirCuantosSonNinosTodosPaganYLaFichaNoTraePrecio() throws Exception {
        buscar(ENTRADA, SALIDA, "2").andExpect(jsonPath("$[0].precio.total").value(400000));
        visitante.perform(get("/api/portal/apartamentos/APT-101")).andExpect(jsonPath("$.precio").isEmpty());
    }

    @Test
    void masNinosQueElGrupoOUnNumeroNegativoSeRechazan() throws Exception {
        visitante.perform(get("/api/portal/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", SALIDA.toString()).param("ocupantes", "2").param("ninos", "-1"))
                .andExpect(status().isBadRequest());
        visitante.perform(get("/api/portal/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", SALIDA.toString()).param("ocupantes", "2").param("ninos", "3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("CU-09")
    @Tag("RN-06")
    void elVisitanteCotizaSinCuentaConElDesglosePorNoche() throws Exception {
        String cotizacion = ("{\"apartamento\": \"APT-101\", \"entrada\": \"%s\", \"salida\": \"%s\","
                + " \"fechasNacimiento\": [\"1990-05-01\", \"1988-03-02\"]}").formatted(ENTRADA, SALIDA);

        visitante.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content(cotizacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.length()").value(2))
                .andExpect(jsonPath("$.desglose[0].ocupantesFacturables").value(2))
                .andExpect(jsonPath("$.total").value(400000));
    }
}
