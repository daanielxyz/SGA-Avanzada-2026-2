package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de la reserva por REST antes de la llegada (B2b). Las reservas entran dentro de 10 días, así que cancelar es
 * gratis (más de 48 h) y el no-show todavía no procede.
 */
class ReservaControllerTest extends PruebaRestDeReservas {

    private static final LocalDate ENTRADA = HOY.plusDays(10);

    @Test
    @Tag("RN-05")
    @Tag("RN-22")
    void deberiaCrearLaReservaPendienteConSuValorCongelado() throws Exception {
        enviar(post("/api/reservas"), reservaJson("APT-101", ENTRADA, ENTRADA.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/reservas/RES-\\d{4}-\\d{5}")))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.apartamento").value("APT-101"))
                .andExpect(jsonPath("$.canalOrigen").value("DIRECTO"))
                .andExpect(jsonPath("$.entrada").value(ENTRADA.toString()))
                .andExpect(jsonPath("$.ocupantes", hasSize(2)))
                .andExpect(jsonPath("$.ocupantes[0].id").value("OCU-1"))
                .andExpect(jsonPath("$.valor.desglose", hasSize(2)))
                .andExpect(jsonPath("$.valor.total").value(400000))
                .andExpect(jsonPath("$.politicaId").value(AlojamientoDePrueba.POLITICA));
    }

    @Test
    void deberiaConsultarLaReservaCreada() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        mvc.perform(get("/api/reservas/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.ocupantes[1].nombre").value("Luis Ríos"));
        mvc.perform(get("/api/reservas/RES-2026-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void deberiaResponder400SiLaReservaTieneMalFormato() throws Exception {
        String invalida = reservaJson("APT-101", ENTRADA, ENTRADA.plusDays(2))
                .replace("\"DIRECTO\"", "\"EXTERNO\"")
                .replace("\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1094\",\n", "\"tipoDocumento\": \"NIT\",\n")
                .replace("ana@correo.co", "ana-sin-arroba");

        enviar(post("/api/reservas"), invalida)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("canalOrigen: debe ser PORTAL o DIRECTO")))
                .andExpect(jsonPath("$.detalles").value(
                        hasItem("titular.tipoDocumento: debe ser CC, CE, PASAPORTE o TI")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.numeroDocumento: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular.correo: no tiene formato de correo")));
        enviar(post("/api/reservas"), "{\"apartamento\": \"APT-101\", \"canalOrigen\": \"PORTAL\", \"ocupantes\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("entrada: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(hasItem("titular: es obligatorio")))
                .andExpect(jsonPath("$.detalles").value(hasItem("ocupantes: debe tener al menos un ocupante")));
    }

    @Test
    @Tag("RN-01")
    void noDeberiaVenderDosVecesLaMismaNoche() throws Exception {
        reservaPendiente(ENTRADA);

        enviar(post("/api/reservas"), reservaJson("APT-101", ENTRADA.plusDays(1), ENTRADA.plusDays(3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("RN-02")
    void unaReglaDeNegocioEs409YUnApartamentoInexistente404() throws Exception {
        String tresEnElDeDos = reservaJson("APT-102", ENTRADA, ENTRADA.plusDays(2)).replace(
                "{\"nombre\": \"Luis Ríos\", \"fechaNacimiento\": \"1988-03-02\"}",
                "{\"nombre\": \"Luis Ríos\", \"fechaNacimiento\": \"1988-03-02\"},"
                        + " {\"nombre\": \"Eva Ríos\", \"fechaNacimiento\": \"2015-01-01\"}");

        enviar(post("/api/reservas"), tresEnElDeDos)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(post("/api/reservas"), reservaJson("APT-101", ENTRADA.plusDays(2), ENTRADA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(post("/api/reservas"), reservaJson("APT-999", ENTRADA, ENTRADA.plusDays(2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @Tag("RN-14")
    void deberiaModificarLaReservaRecalculandoSuValor() throws Exception {
        String codigo = reservaPendiente(ENTRADA);
        String tresNoches = """
                {
                  "apartamento": "APT-101",
                  "entrada": "%s",
                  "salida": "%s",
                  "ocupantes": [
                    {"nombre": "Ana Ríos", "fechaNacimiento": "1990-05-01", "tipoDocumento": "CC",
                     "numeroDocumento": "1094"}
                  ]
                }
                """.formatted(ENTRADA, ENTRADA.plusDays(3));

        enviar(put("/api/reservas/" + codigo + "/modificar"), tresNoches)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salida").value(ENTRADA.plusDays(3).toString()))
                .andExpect(jsonPath("$.ocupantes", hasSize(1)))
                .andExpect(jsonPath("$.valor.total").value(300000));
        mvc.perform(get("/api/reservas/" + codigo + "/folio"))
                .andExpect(jsonPath("$.saldo").value(300000))
                .andExpect(jsonPath("$.cargos[1].tipo").value("AJUSTE"));
        enviar(put("/api/reservas/" + codigo + "/modificar"), "{\"apartamento\": \"APT-101\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("salida: es obligatoria")));
    }

    @Test
    @Tag("RN-09")
    @Tag("RES-15")
    void confirmarExigeLaHoraDeLlegadaYElAnticipo() throws Exception {
        String codigo = reservaPendiente(ENTRADA);
        String confirmar = "/api/reservas/" + codigo + "/confirmar";

        mvc.perform(put(confirmar)).andExpect(status().isConflict());
        enviar(put("/api/reservas/" + codigo + "/hora-llegada"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("hora: es obligatoria")));
        enviar(put("/api/reservas/" + codigo + "/hora-llegada"), "{\"hora\": \"16:30\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaEstimadaLlegada").value("16:30:00"));
        mvc.perform(put(confirmar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        pagar(codigo, 120_000);

        mvc.perform(put(confirmar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @Tag("RN-13")
    void deberiaCancelarSinPenalidadYDejarElAnticipoAFavor() throws Exception {
        String codigo = reservaConfirmada(ENTRADA);

        mvc.perform(put("/api/reservas/" + codigo + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.retenido").value(0))
                .andExpect(jsonPath("$.saldo").value(120000))
                .andExpect(jsonPath("$.situacionSaldo").value("A_FAVOR"));
        mvc.perform(put("/api/reservas/" + codigo + "/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put("/api/reservas/RES-2026-99999/cancelar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("RES-16")
    void elNoShowNoProcedeAntesDelDiaDeEntrada() throws Exception {
        String codigo = reservaConfirmada(ENTRADA);

        mvc.perform(put("/api/reservas/" + codigo + "/no-show"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(get("/api/reservas/" + codigo))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }
}
