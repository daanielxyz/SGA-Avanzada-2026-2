package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Búsqueda de apartamentos libres y cotización por REST (B2b). Hay dos apartamentos: APT-101 (4) y APT-102 (2).
 */
class DisponibilidadControllerTest extends PruebaRestDeReservas {

    private static final LocalDate ENTRADA = HOY.plusDays(10);

    @Test
    @Tag("DISP-02")
    void deberiaOfrecerLosApartamentosLibresDondeCabeElGrupo() throws Exception {
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].codigo").value(hasItem("APT-102")));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("APT-101"))
                .andExpect(jsonPath("$[0].capacidad").value(4));
    }

    @Test
    @Tag("RN-01")
    void unApartamentoReservadoDejaDeOfrecerseEnEsasNoches() throws Exception {
        reservaPendiente(ENTRADA);

        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.plusDays(1).toString())
                        .param("salida", ENTRADA.plusDays(4).toString()).param("ocupantes", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("APT-102"));
    }

    @Test
    void deberiaResponder400SiFaltanOSonInvalidosLosParametrosDeBusqueda() throws Exception {
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString()).param("ocupantes", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("salida: es obligatorio")));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", "mañana")
                        .param("salida", ENTRADA.toString()).param("ocupantes", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("entrada: tiene un formato inválido")));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(2).toString()).param("ocupantes", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("ocupantes: debe ser al menos 1")));
    }

    @Test
    @Tag("RN-06")
    @Tag("COT-02")
    void laCotizacionSoloCobraLosOcupantesFacturablesYNoGuardaNada() throws Exception {
        String cotizacion = """
                {
                  "apartamento": "APT-101",
                  "entrada": "%s",
                  "salida": "%s",
                  "fechasNacimiento": ["1990-05-01", "1988-03-02", "2021-01-01"]
                }
                """.formatted(ENTRADA, ENTRADA.plusDays(3));

        enviar(post("/api/cotizaciones"), cotizacion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose", hasSize(3)))
                .andExpect(jsonPath("$.desglose[0].ocupantesFacturables").value(2))
                .andExpect(jsonPath("$.desglose[0].tarifa").value(100000))
                .andExpect(jsonPath("$.total").value(600000));
        mvc.perform(get("/api/apartamentos/disponibles").param("entrada", ENTRADA.toString())
                        .param("salida", ENTRADA.plusDays(3).toString()).param("ocupantes", "3"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unaCotizacionMalFormadaEs400YDeUnApartamentoInexistente404() throws Exception {
        enviar(post("/api/cotizaciones"),
                "{\"apartamento\": \"APT-101\", \"entrada\": \"%s\", \"fechasNacimiento\": []}".formatted(ENTRADA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("salida: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(
                        hasItem("fechasNacimiento: debe tener al menos una fecha de nacimiento")));
        enviar(post("/api/cotizaciones"), ("{\"apartamento\": \"APT-999\", \"entrada\": \"%s\", \"salida\": \"%s\","
                + " \"fechasNacimiento\": [\"1990-05-01\"]}").formatted(ENTRADA, ENTRADA.plusDays(2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
