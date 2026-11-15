package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.como;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La lista general de novedades por REST (DEC-132), sobre el APT-101 y el APT-102 de {@link PruebaRestDeReservas}.
 */
class ListarNovedadesTest extends PruebaRestDeReservas {

    private String grifo;
    private String vidrio;
    private String toalla;

    @BeforeEach
    void registrarNovedades() throws Exception {
        grifo = registrar("APT-101", "Grifo del baño gotea", "MEDIA");
        vidrio = registrar("APT-102", "Vidrio de la ventana roto", "ALTA");
        toalla = registrar("APT-102", "Falta una toalla", "BAJA");
        mvc.perform(put("/api/novedades/" + vidrio + "/avanzar")).andExpect(status().isOk());
    }

    private String registrar(String apartamento, String descripcion, String gravedad) throws Exception {
        String respuesta = enviar(post("/api/apartamentos/" + apartamento + "/novedades"),
                "{\"descripcion\": \"%s\", \"gravedad\": \"%s\"}".formatted(descripcion, gravedad))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @Tag("DEC-132")
    void sinFiltrosTraeLasDeTodosLosApartamentosConElNombreDeCadaAutor() throws Exception {
        mvc.perform(get("/api/novedades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[*].id").value(containsInAnyOrder(grifo, vidrio, toalla)))
                .andExpect(jsonPath("$.contenido[0].historial[0]").value(hasKey("autorNombre")));
    }

    @Test
    @Tag("DEC-132")
    @Tag("DEC-119")
    void cadaFiltroAdmiteVariosValoresYEntreFiltrosEsY() throws Exception {
        mvc.perform(get("/api/novedades").param("estado", "ABIERTA"))
                .andExpect(jsonPath("$.contenido[*].id").value(containsInAnyOrder(grifo, toalla)));
        mvc.perform(get("/api/novedades").param("estado", "ABIERTA,EN_REVISION").param("apartamento", "APT-102"))
                .andExpect(jsonPath("$.contenido[*].id").value(containsInAnyOrder(vidrio, toalla)));
        mvc.perform(get("/api/novedades").param("gravedad", "ALTA").param("gravedad", "MEDIA")
                        .param("estado", "EN_REVISION"))
                .andExpect(jsonPath("$.contenido[*].id").value(containsInAnyOrder(vidrio)));
        mvc.perform(get("/api/novedades").param("estado", "CERRADA"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @Tag("DEC-132")
    void unValorQueNoExisteEs400() throws Exception {
        mvc.perform(get("/api/novedades").param("estado", "URGENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("estado: debe ser ABIERTA, EN_REVISION o CERRADA")));
        mvc.perform(get("/api/novedades").param("gravedad", "CRITICA"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/novedades").param("apartamento", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("DEC-132")
    void elPersonalDeServicioLaVeYRecepcionNo() throws Exception {
        mvc.perform(get("/api/novedades").with(como(Rol.PERSONAL_SERVICIO)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/novedades").with(como(Rol.RECEPCIONISTA)))
                .andExpect(status().isForbidden());
    }
}
