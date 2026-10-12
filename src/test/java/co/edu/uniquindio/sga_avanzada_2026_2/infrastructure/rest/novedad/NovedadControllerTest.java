package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Novedades de un apartamento por REST (B2c), sobre el APT-101 de {@link PruebaRestDeReservas}, que está PREPARADO.
 */
class NovedadControllerTest extends PruebaRestDeReservas {

    private static final String NOVEDADES = "/api/apartamentos/APT-101/novedades";
    private static final String GRIFO = "{\"descripcion\": \"Grifo del baño gotea\", \"gravedad\": \"MEDIA\"}";

    private String registrarGrifo() throws Exception {
        String respuesta = enviar(post(NOVEDADES), GRIFO)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @Tag("NOV-01")
    @Tag("NOV-03")
    void deberiaRegistrarUnaNovedadAbiertaSinTocarElApartamento() throws Exception {
        enviar(post(NOVEDADES), GRIFO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apartamento").value("APT-101"))
                .andExpect(jsonPath("$.gravedad").value("MEDIA"))
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.historial", hasSize(1)))
                .andExpect(jsonPath("$.historial[0].autor").value(UsuarioActual.PROVISIONAL));
        mvc.perform(get("/api/apartamentos/APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"))
                .andExpect(jsonPath("$.bloqueos").isEmpty());
    }

    @Test
    @Tag("NOV-02")
    void unaNovedadMalFormadaEs400YDeUnApartamentoInexistente404() throws Exception {
        enviar(post(NOVEDADES), "{\"descripcion\": \" \", \"gravedad\": \"URGENTE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("descripcion: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(hasItem("gravedad: debe ser BAJA, MEDIA o ALTA")));
        enviar(post("/api/apartamentos/APT-999/novedades"), GRIFO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        mvc.perform(get("/api/apartamentos/APT-999/novedades"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("NOV-04")
    @Tag("NOV-06")
    void deberiaAvanzarLaNovedadHastaCerrarla() throws Exception {
        String avanzar = "/api/novedades/" + registrarGrifo() + "/avanzar";

        mvc.perform(put(avanzar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        mvc.perform(put(avanzar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.historial", hasSize(3)))
                .andExpect(jsonPath("$.historial[2].estado").value("CERRADA"));
        mvc.perform(put(avanzar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put("/api/novedades/NOV-99999/avanzar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("NOV-06")
    @Tag("DEC-18")
    void deberiaListarElHistorialDelApartamentoPorPaginas() throws Exception {
        String grifo = registrarGrifo();
        enviar(post(NOVEDADES), "{\"descripcion\": \"Falta una toalla\", \"gravedad\": \"BAJA\"}")
                .andExpect(status().isCreated());
        mvc.perform(put("/api/novedades/" + grifo + "/avanzar")).andExpect(status().isOk());

        mvc.perform(get(NOVEDADES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[*].estado").value(hasItem("EN_REVISION")))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1));
        mvc.perform(get(NOVEDADES).param("pagina", "1"))
                .andExpect(jsonPath("$.contenido").isEmpty());
        mvc.perform(get(NOVEDADES).param("pagina", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("pagina: no puede ser negativa")));
    }
}
