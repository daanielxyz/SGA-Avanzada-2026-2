package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los 409 que ve recepción se leen sin códigos internos: estados en palabras, dinero en pesos y sin ids de pagos,
 * cargos o folios (#8 · DEC-118). El {@code motivo} no cambia (DEC-94).
 */
class MensajesRechazoTest extends PruebaRestDeReservas {

    @Test
    @Tag("DEC-118")
    void elEstadoDeLaReservaSeDiceEnPalabras() throws Exception {
        String codigo = reservaEnCurso();

        enviar(put("/api/reservas/" + codigo + "/modificar"), reservaJson("APT-101", HOY, HOY.plusDays(3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.mensaje").value("La reserva está en curso: solo se modifica antes de la "
                        + "llegada, mientras está pendiente o confirmada"));
        mvc.perform(put("/api/reservas/" + codigo + "/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(not(containsString("EN_CURSO"))))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("→"))));
    }

    @Test
    @Tag("DEC-118")
    void elDineroSeLeeEnPesos() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        enviar(put("/api/reservas/" + codigo + "/hora-llegada"), "{\"hora\": \"16:00\"}").andExpect(status().isOk());
        pagar(codigo, 50_000);

        mvc.perform(put("/api/reservas/" + codigo + "/confirmar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ANTICIPO_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje")
                        .value("Lo pagado ($ 50.000) no cubre el anticipo exigido ($ 120.000)"));
    }

    @Test
    @Tag("DEC-118")
    void unPagoYaRevertidoNoSeNombraPorSuId() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        pagar(codigo, 50_000);
        mvc.perform(post("/api/reservas/" + codigo + "/folio/pagos/PAG-1/revertir")).andExpect(status().isCreated());

        mvc.perform(post("/api/reservas/" + codigo + "/folio/pagos/PAG-1/revertir"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("YA_REVERTIDO"))
                .andExpect(jsonPath("$.mensaje").value("Ese pago ya fue revertido"));
    }
}
