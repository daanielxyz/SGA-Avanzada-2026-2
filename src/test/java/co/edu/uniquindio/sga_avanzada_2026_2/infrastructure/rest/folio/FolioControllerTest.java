package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoDePrueba;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Folio de una reserva por REST (B2b). La reserva es de 400.000 y el anticipo exigido, 120.000; el alojamiento
 * acepta EFECTIVO y TARJETA y tiene el Desayuno a 25.000.
 */
class FolioControllerTest extends PruebaRestDeReservas {

    private static final LocalDate ENTRADA = HOY.plusDays(10);

    private static String folio(String codigo) {
        return "/api/reservas/" + codigo + "/folio";
    }

    @Test
    @Tag("FOL-01")
    @Tag("FOL-03")
    void deberiaConsultarElFolioQueNacioConLaReserva() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        mvc.perform(get(folio(codigo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserva").value(codigo))
                .andExpect(jsonPath("$.cargos", hasSize(1)))
                .andExpect(jsonPath("$.cargos[0].tipo").value("HOSPEDAJE"))
                .andExpect(jsonPath("$.pagos").isEmpty())
                .andExpect(jsonPath("$.saldo").value(400000))
                .andExpect(jsonPath("$.situacionSaldo").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrado").value(false))
                .andExpect(jsonPath("$.autorizacion").value(nullValue()));
        mvc.perform(get(folio("RES-2026-99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @Tag("RN-15")
    @Tag("PAG-05")
    void deberiaRegistrarPagosParcialesConMediosDistintos() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        enviar(post(folio(codigo) + "/pagos"), pagoJson("EFECTIVO", 150_000, HOY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagos[0].id").value("PAG-1"))
                .andExpect(jsonPath("$.pagos[0].tipo").value("ABONO"));
        enviar(post(folio(codigo) + "/pagos"), pagoJson("tarjeta", 50_000, HOY.minusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagos[1].medio").value("TARJETA"))
                .andExpect(jsonPath("$.totalPagado").value(200000))
                .andExpect(jsonPath("$.saldo").value(200000));
    }

    @Test
    @Tag("MPAG-02")
    @Tag("PAG-07")
    void unPagoMalFormadoEs400YUnoQueVioleUnaReglaEs409() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        enviar(post(folio(codigo) + "/pagos"), "{\"medio\": \"EFECTIVO\", \"monto\": 100.50}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("fecha: es obligatoria")))
                .andExpect(jsonPath("$.detalles").value(
                        hasItem("monto: debe ser un valor en pesos sin decimales")));
        enviar(post(folio(codigo) + "/pagos"), pagoJson("EFECTIVO", 0, HOY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("monto: debe ser mayor que cero")));
        enviar(post(folio(codigo) + "/pagos"), pagoJson("CRIPTO", 100_000, HOY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(post(folio(codigo) + "/pagos"), pagoJson("EFECTIVO", 100_000, HOY.plusDays(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        mvc.perform(get(folio(codigo))).andExpect(jsonPath("$.pagos").isEmpty());
    }

    @Test
    @Tag("RN-16")
    @Tag("PAG-03")
    void deberiaRevertirUnPagoSinBorrarlo() throws Exception {
        String codigo = reservaPendiente(ENTRADA);
        pagar(codigo, 150_000);

        mvc.perform(post(folio(codigo) + "/pagos/PAG-1/revertir"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagos", hasSize(2)))
                .andExpect(jsonPath("$.pagos[1].tipo").value("REVERSO"))
                .andExpect(jsonPath("$.pagos[1].reversaDe").value("PAG-1"))
                .andExpect(jsonPath("$.totalPagado").value(0));
        mvc.perform(post(folio(codigo) + "/pagos/PAG-1/revertir"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    @Tag("PAG-06")
    void deberiaRegistrarLaDevolucionDelSaldoAFavorTrasCancelar() throws Exception {
        String codigo = reservaConfirmada(ENTRADA);
        mvc.perform(put("/api/reservas/" + codigo + "/cancelar")).andExpect(status().isOk());

        enviar(post(folio(codigo) + "/devoluciones"), pagoJson("EFECTIVO", 500_000, HOY))
                .andExpect(status().isConflict());
        enviar(post(folio(codigo) + "/devoluciones"), pagoJson("EFECTIVO", 120_000, HOY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagos[1].tipo").value("REVERSO"))
                .andExpect(jsonPath("$.pagos[1].reversaDe").value(nullValue()))
                .andExpect(jsonPath("$.situacionSaldo").value("AL_DIA"));
    }

    @Test
    @Tag("CAR-05")
    @Tag("CAR-03")
    void deberiaCobrarUnServicioDuranteLaEstanciaYCorregirlo() throws Exception {
        String codigo = reservaEnCurso();
        String servicio = "{\"servicioId\": \"%s\"}".formatted(AlojamientoDePrueba.DESAYUNO);

        enviar(post(folio(codigo) + "/servicios"), servicio)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargos[1].tipo").value("SERVICIO_ADICIONAL"))
                .andExpect(jsonPath("$.cargos[1].concepto").value("Desayuno"))
                .andExpect(jsonPath("$.cargos[1].valor").value(25000))
                .andExpect(jsonPath("$.saldo").value(305000));
        enviar(post(folio(codigo) + "/cargos/CAR-2/revertir"), "{\"motivo\": \"Era de otro apartamento\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargos[2].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.cargos[2].sentido").value("DISMINUYE"))
                .andExpect(jsonPath("$.cargos[2].corrigeA").value("CAR-2"))
                .andExpect(jsonPath("$.saldo").value(280000));
        enviar(post(folio(codigo) + "/cargos/CAR-2/revertir"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("motivo: es obligatorio")));
    }

    @Test
    @Tag("DEC-60")
    void noDeberiaCobrarUnServicioAntesDeLaLlegada() throws Exception {
        String codigo = reservaConfirmada(ENTRADA);

        enviar(post(folio(codigo) + "/servicios"), "{\"servicioId\": \"%s\"}".formatted(AlojamientoDePrueba.DESAYUNO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(post(folio(codigo) + "/servicios"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("servicioId: es obligatorio")));
    }

    @Test
    @Tag("RN-17")
    @Tag("FOL-06")
    void cerrarExigeSaldoEnCeroYLuegoNoAdmiteMovimientos() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        mvc.perform(put(folio(codigo) + "/cerrar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        pagar(codigo, 400_000);
        mvc.perform(put(folio(codigo) + "/cerrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.autorizacion").value(nullValue()));
        enviar(post(folio(codigo) + "/pagos"), pagoJson("EFECTIVO", 1_000, HOY))
                .andExpect(status().isConflict());
    }

    @Test
    @Tag("RN-17")
    @Tag("AUTC-04")
    void deberiaCerrarConSaldoSiLlegaElMotivoDeLaAutorizacion() throws Exception {
        String codigo = reservaPendiente(ENTRADA);

        enviar(put(folio(codigo) + "/cerrar"), "{\"motivoAutorizacion\": \"Convenio con la empresa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.saldo").value(400000))
                .andExpect(jsonPath("$.autorizacion.motivo").value("Convenio con la empresa"))
                .andExpect(jsonPath("$.autorizacion.autor").value(SesionDePrueba.ADMINISTRADOR));
    }
}
