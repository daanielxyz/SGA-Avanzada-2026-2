package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.registros[0].autor").value(SesionDePrueba.ADMINISTRADOR))
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
                .andExpect(jsonPath("$.salida.autor").value(SesionDePrueba.ADMINISTRADOR))
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

    @Test
    @Tag("BL-07")
    void laSalidaEsperaLosDatosDelTraQueRecepcionCompletaPorPartes() throws Exception {
        String codigo = reservaConfirmada(HOY);
        mvc.perform(put(reserva(codigo) + "/registrar-llegada")).andExpect(status().isOk());
        pagar(codigo, 280_000);
        mvc.perform(get(reserva(codigo)))
                .andExpect(jsonPath("$.ocupantes[0].nombre").value("Ana Ríos"))
                .andExpect(jsonPath("$.ocupantes[1].datosTraFaltantes[0]").value("DOCUMENTO"))
                .andExpect(jsonPath("$.ocupantes[1].datosTraFaltantes.length()").value(9));

        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("DATOS_TRA_INCOMPLETOS"));

        completarDatosTra(codigo, "OCU-1", "{\"genero\": \"NINGUNO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(startsWith("genero")));
        completarDatosTra(codigo, "OCU-1", "{\"tipoDocumento\": \"CC\"}").andExpect(status().isBadRequest());
        completarDatosTra(codigo, "OCU-9", "{\"primerApellido\": \"Ríos\"}").andExpect(status().isConflict());
        completarDatosTra(codigo, "OCU-1", datosTra("Ana", "FEMENINO", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.ocupantes[0].primerApellido").value("Ríos"))
                .andExpect(jsonPath("$.ocupantes[0].motivoViaje").value("TURISMO_VACACIONES"))
                .andExpect(jsonPath("$.ocupantes[0].datosTraFaltantes.length()").value(0));
        completarDatosTra(codigo, "OCU-2", "{\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"1095\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes[1].datosTraFaltantes[0]").value("PRIMER_APELLIDO"));
        mvc.perform(put(reserva(codigo) + "/registrar-salida")).andExpect(status().isConflict());

        completarDatosTra(codigo, "OCU-2", datosTra("Luis", "MASCULINO", null)).andExpect(status().isOk());
        mvc.perform(put(reserva(codigo) + "/registrar-salida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"));
        completarDatosTra(codigo, "OCU-2", DATOS_ULTIMO).andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"));
    }

    private static final String DATOS_ULTIMO = "{\"ciudadDestino\": \"Armenia\"}";

    @Test
    @Tag("BL-07")
    void unaReservaAceptaLosApellidosAparteDesdeQueSeCrea() throws Exception {
        String json = reservaJson("APT-101", HOY, HOY.plusDays(2)).replace(
                "{\"nombre\": \"Luis Ríos\", \"fechaNacimiento\": \"1988-03-02\"}",
                "{\"nombres\": \"Luis\", \"primerApellido\": \"Ríos\", \"segundoApellido\": \"Gil\", "
                        + "\"fechaNacimiento\": \"1988-03-02\"}");
        enviar(post("/api/reservas"), json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupantes[1].nombre").value("Luis Ríos Gil"))
                .andExpect(jsonPath("$.ocupantes[1].segundoApellido").value("Gil"));
        enviar(post("/api/reservas"), json.replace("\"nombres\": \"Luis\", ", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("TIT-01")
    void recepcionCambiaElTitularPorAlguienQueNoSeAloja() throws Exception {
        String codigo = reservaConfirmada(HOY);
        String marta = """
                {"nombre": "Marta Ríos", "tipoDocumento": "CC", "numeroDocumento": "3030",
                 "correo": "marta@correo.co", "telefono": "300 765 4321"}""";

        String nuevo = JsonPath.read(enviar(put(reserva(codigo) + "/cambiar-titular"), marta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes.length()").value(2))
                .andReturn().getResponse().getContentAsString(), "$.titularId");
        mvc.perform(get(reserva(codigo)))
                .andExpect(jsonPath("$.titularId").value(nuevo))
                .andExpect(jsonPath("$.titularNombre").value("Marta Ríos"));

        enviar(put(reserva(codigo) + "/cambiar-titular"), marta)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("TITULAR_INVALIDO"));
        enviar(put(reserva(codigo) + "/cambiar-titular"), "{\"nombre\": \"Sin documento\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("DEC-97")
    void recepcionDesignaOtroHuespedPrincipal() throws Exception {
        String codigo = reservaConfirmada(HOY);
        mvc.perform(get(reserva(codigo)))
                .andExpect(jsonPath("$.ocupantes[0].principal").value(true))
                .andExpect(jsonPath("$.ocupantes[1].principal").value(false));

        mvc.perform(put(reserva(codigo) + "/ocupantes/OCU-2/principal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes[0].principal").value(false))
                .andExpect(jsonPath("$.ocupantes[1].principal").value(true));
        mvc.perform(get(reserva(codigo))).andExpect(jsonPath("$.ocupantes[1].principal").value(true));

        mvc.perform(put(reserva(codigo) + "/ocupantes/OCU-2/principal"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("HUESPED_PRINCIPAL_INVALIDO"));
    }

    @Test
    @Tag("DEC-97")
    void alReservarSeMarcaAlHuespedPrincipalYDebeSerMayorDeEdad() throws Exception {
        String json = reservaJson("APT-101", HOY, HOY.plusDays(2));
        enviar(post("/api/reservas"), json.replace("\"1988-03-02\"}", "\"1988-03-02\", \"principal\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupantes[0].principal").value(false))
                .andExpect(jsonPath("$.ocupantes[1].principal").value(true));
        String otraFecha = reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7));
        enviar(post("/api/reservas"), otraFecha.replace("1988-03-02\"}", "2015-03-02\", \"principal\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("HUESPED_PRINCIPAL_INVALIDO"));
    }

    @Test
    @Tag("TIT-01")
    void unaReservaSePuedeHacerParaOtrosSinQueElTitularSeAloje() throws Exception {
        String json = reservaJson("APT-101", HOY, HOY.plusDays(2)).replace("""
                    {"nombre": "Ana Ríos", "fechaNacimiento": "1990-05-01", "tipoDocumento": "CC",
                     "numeroDocumento": "1094", "nacionalidad": "Colombiana"},
                """, "");
        enviar(post("/api/reservas"), json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ocupantes.length()").value(1))
                .andExpect(jsonPath("$.ocupantes[0].nombre").value("Luis Ríos"));
    }
}
