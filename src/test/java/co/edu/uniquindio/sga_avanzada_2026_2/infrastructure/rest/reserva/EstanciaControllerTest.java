package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Llegada, corrección del registro y salida por REST (B2b). La reserva entra hoy, vale 400.000 y tiene pagado el
 * anticipo de 120.000: al llegar debe 280.000.
 */
class EstanciaControllerTest extends PruebaRestDeReservas {

    private static String reserva(String codigo) {
        return "/api/reservas/" + codigo;
    }

    @Test
    @Tag("RN-10")
    @Tag("RN-11")
    @Tag("REG-04")
    void deberiaRegistrarLaLlegadaYOcuparElApartamento() throws Exception {
        String codigo = reservaConfirmada(HOY);

        mvc.perform(put(reserva(codigo) + "/registrar-llegada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.apartamento").value("APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("OCUPADO"))
                .andExpect(jsonPath("$.registros", hasSize(1)))
                .andExpect(jsonPath("$.registros[0].id").value("REG-1"))
                .andExpect(jsonPath("$.registros[0].autor").value(UsuarioActual.PROVISIONAL))
                .andExpect(jsonPath("$.salida").value(nullValue()))
                .andExpect(jsonPath("$.folioCerrado").value(false))
                .andExpect(jsonPath("$.saldo").value(280000));
        mvc.perform(get("/api/apartamentos/APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("OCUPADO"));
    }

    @Test
    @Tag("RN-10")
    void noDeberiaRegistrarLaLlegadaSinConfirmarNiAntesDeLaFecha() throws Exception {
        String pendiente = reservaPendiente(HOY);
        String futura = reservaConfirmada(HOY.plusDays(10));

        mvc.perform(put(reserva(pendiente) + "/registrar-llegada"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put(reserva(futura) + "/registrar-llegada"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(put(reserva("RES-2026-99999") + "/registrar-llegada"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/apartamentos/APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    @Tag("REG-06")
    void deberiaCorregirElRegistroConservandoElAnterior() throws Exception {
        String codigo = reservaEnCurso();
        LocalDateTime llegadaReal = LocalDateTime.now(ZoneId.of("America/Bogota")).truncatedTo(ChronoUnit.SECONDS);
        String correccion = "{\"fechaHora\": \"%s\", \"motivo\": \"Se digitó mal la hora\"}";

        enviar(put(reserva(codigo) + "/corregir-registro"), correccion.formatted(llegadaReal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.registros", hasSize(2)))
                .andExpect(jsonPath("$.registros[0].anulado").value(true))
                .andExpect(jsonPath("$.registros[1].id").value("REG-2"))
                .andExpect(jsonPath("$.registros[1].anulado").value(false))
                .andExpect(jsonPath("$.registros[1].motivoCorreccion").value("Se digitó mal la hora"));
        enviar(put(reserva(codigo) + "/corregir-registro"), correccion.formatted(llegadaReal.plusDays(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put(reserva(codigo) + "/corregir-registro"), "{\"fechaHora\": \"ayer\", \"motivo\": \"x\"}")
                .andExpect(status().isBadRequest());
        enviar(put(reserva(codigo) + "/corregir-registro"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("fechaHora: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(hasItem("motivo: es obligatorio")));
    }

    @Test
    @Tag("RN-17")
    @Tag("SAL-02")
    @Tag("SAL-03")
    void laSalidaExigeElFolioEnCeroYLiberaElApartamento() throws Exception {
        String codigo = reservaEnCurso();

        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(get(reserva(codigo))).andExpect(jsonPath("$.estado").value("EN_CURSO"));
        pagar(codigo, 280_000);

        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"))
                .andExpect(jsonPath("$.salida.id").value("SAL-1"))
                .andExpect(jsonPath("$.salida.autor").value(UsuarioActual.PROVISIONAL))
                .andExpect(jsonPath("$.folioCerrado").value(true))
                .andExpect(jsonPath("$.situacionSaldo").value("AL_DIA"));
        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isConflict());
        mvc.perform(get(reserva(codigo) + "/folio")).andExpect(jsonPath("$.cerrado").value(true));
    }

    @Test
    @Tag("RN-17")
    @Tag("AUTC-04")
    void deberiaRegistrarLaSalidaConSaldoSiLlegaElMotivoDeLaAutorizacion() throws Exception {
        String codigo = reservaEnCurso();

        enviar(put(reserva(codigo) + "/registrar-salida"),
                "{\"motivoAutorizacion\": \"Pagará por transferencia el lunes\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.folioCerrado").value(true))
                .andExpect(jsonPath("$.saldo").value(280000))
                .andExpect(jsonPath("$.situacionSaldo").value("PENDIENTE"));
        mvc.perform(get(reserva(codigo) + "/folio"))
                .andExpect(jsonPath("$.autorizacion.motivo").value("Pagará por transferencia el lunes"));
    }

    @Test
    @Tag("SAL-01")
    void noDeberiaRegistrarLaSalidaDeUnaReservaQueNoHaLlegado() throws Exception {
        String codigo = reservaConfirmada(HOY);

        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }
}
