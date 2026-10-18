package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El listado de reservas de recepción con filtros (B3e · CU-25 · DEC-72). Tres reservas: Ana en el APT-101 del día 5
 * al 7 (PENDIENTE) y del 10 al 12 (CONFIRMADA), y Eva en el APT-102 del 5 al 6 (PENDIENTE); los días cuentan desde hoy.
 */
class ListadoReservasTest extends PruebaRestDeReservas {

    private String anaPendiente;
    private String anaConfirmada;
    private String eva;
    private String titularDeEva;

    @BeforeEach
    void prepararReservas() throws Exception {
        anaPendiente = reservaPendiente(HOY.plusDays(5));
        anaConfirmada = reservaConfirmada(HOY.plusDays(10));
        String json = reservaJson("APT-102", HOY.plusDays(5), HOY.plusDays(6)).replace("1094", "2077")
                .replace("ana@correo.co", "eva@correo.co").replace("Ana Ríos", "Eva Mora");
        String respuesta = enviar(post("/api/reservas"), json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        eva = JsonPath.read(respuesta, "$.codigo");
        titularDeEva = JsonPath.read(respuesta, "$.titularId");
    }

    private ResultActions listar(String... filtros) throws Exception {
        var peticion = get("/api/reservas");
        for (int i = 0; i < filtros.length; i += 2) {
            peticion.param(filtros[i], filtros[i + 1]);
        }
        return mvc.perform(peticion);
    }

    private static String dia(int dias) {
        return HOY.plusDays(dias).toString();
    }

    @Test
    @Tag("CU-25")
    @Tag("DEC-18")
    void sinFiltrosListaTodasDeLaQueEntraPrimero() throws Exception {
        listar()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaPendiente, eva, anaConfirmada)));
        listar("pagina", "1").andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test
    @Tag("CU-25")
    void cadaFiltroRestringeYSeCombinan() throws Exception {
        listar("estado", "CONFIRMADA").andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaConfirmada)));
        listar("apartamento", "APT-102").andExpect(jsonPath("$.contenido[*].codigo").value(contains(eva)));
        listar("titular", titularDeEva).andExpect(jsonPath("$.contenido[*].codigo").value(contains(eva)));
        listar("canal", "PORTAL").andExpect(jsonPath("$.contenido").isEmpty());
        listar("canal", "DIRECTO", "apartamento", "APT-101", "estado", "PENDIENTE")
                .andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaPendiente)));
    }

    @Test
    @Tag("EST-03")
    @Tag("DEC-72")
    void porFechasEntraLaQueSeCruzaConElRango() throws Exception {
        listar("desde", dia(6), "hasta", dia(11))
                .andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaPendiente, anaConfirmada)));
        listar("desde", dia(7), "hasta", dia(10)).andExpect(jsonPath("$.contenido").isEmpty());
        listar("hasta", dia(6)).andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaPendiente, eva)));
        listar("desde", dia(11)).andExpect(jsonPath("$.contenido[*].codigo").value(contains(anaConfirmada)));
    }

    @Test
    @Tag("DEC-72")
    void unFiltroMalFormadoEs400YUnRangoAlReves409() throws Exception {
        listar("estado", "RARA", "canal", "WHATSAPP")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem(
                        "estado: debe ser PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA o NO_SHOW")))
                .andExpect(jsonPath("$.detalles").value(hasItem("canal: debe ser PORTAL, DIRECTO o EXTERNO")));
        listar("desde", "ayer").andExpect(status().isBadRequest());
        listar("desde", dia(8), "hasta", dia(8))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("ROL-04")
    @Tag("USU-04")
    void soloRecepcionYAdministracionVenElListado() throws Exception {
        mvc.perform(get("/api/reservas").with(SesionDePrueba.como(Rol.RECEPCIONISTA))).andExpect(status().isOk());
        for (Rol rol : List.of(Rol.HUESPED, Rol.PERSONAL_SERVICIO)) {
            mvc.perform(get("/api/reservas").with(SesionDePrueba.como(rol))).andExpect(status().isForbidden());
        }
    }
}
