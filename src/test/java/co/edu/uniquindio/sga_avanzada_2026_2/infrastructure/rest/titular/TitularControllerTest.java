package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Titulares por REST (B2c), en el alojamiento de {@link PruebaRestDeReservas}.
 */
class TitularControllerTest extends PruebaRestDeReservas {

    private static final String ANA = """
            {"nombre": "Ana Ríos", "tipoDocumento": "CC", "numeroDocumento": "1094", "correo": "ana@correo.co"}
            """;

    private String registrarAna() throws Exception {
        String respuesta = enviar(post("/api/titulares"), ANA)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @Tag("TIT-03")
    void deberiaRegistrarYConsultarUnTitularSinReserva() throws Exception {
        enviar(post("/api/titulares"), ANA)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/titulares/TIT-\\d+")))
                .andExpect(jsonPath("$.nombre").value("Ana Ríos"))
                .andExpect(jsonPath("$.numeroDocumento").value("1094"))
                .andExpect(jsonPath("$.telefono").value(nullValue()));
        mvc.perform(get("/api/titulares/TIT-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @Tag("TIT-05")
    @Tag("DEC-63")
    void unDocumentoRepetidoEs409Duplicado() throws Exception {
        String id = registrarAna();

        enviar(post("/api/titulares"), ANA.replace("Ana Ríos", "Ana María"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"));
        mvc.perform(get("/api/titulares/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Ríos"));
    }

    @Test
    @Tag("DEC-49")
    void unTitularMalFormadoEs400YSinContactoEs409() throws Exception {
        enviar(post("/api/titulares"), "{\"nombre\": \"Ana\", \"tipoDocumento\": \"NIT\", \"correo\": \"ana\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("tipoDocumento: debe ser CC, CE, PASAPORTE o TI")))
                .andExpect(jsonPath("$.detalles").value(hasItem("numeroDocumento: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("correo: no tiene formato de correo")));
        enviar(post("/api/titulares"), "{\"nombre\": \"Ana\", \"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("TIT-01")
    void deberiaActualizarNombreYContactoSinCambiarElDocumento() throws Exception {
        String id = registrarAna();

        enviar(put("/api/titulares/" + id), "{\"nombre\": \"Ana María Ríos\", \"telefono\": \"+57 300 123 4567\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María Ríos"))
                .andExpect(jsonPath("$.correo").value(nullValue()))
                .andExpect(jsonPath("$.telefono").value("+57 300 123 4567"))
                .andExpect(jsonPath("$.numeroDocumento").value("1094"));
        enviar(put("/api/titulares/" + id), "{\"nombre\": \"Ana\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put("/api/titulares/" + id), "{\"telefono\": \"300 123 4567\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("nombre: es obligatorio")));
        enviar(put("/api/titulares/TIT-99999"), "{\"nombre\": \"Ana\", \"telefono\": \"300 123 4567\"}")
                .andExpect(status().isNotFound());
    }
}
