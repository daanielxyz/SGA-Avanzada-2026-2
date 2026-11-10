package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notas de la reserva para el siguiente turno de extremo a extremo (BL-08 · DEC-105). Entra el administrador de
 * {@link SesionDePrueba}, salvo donde se dice otro rol.
 */
class NotaReservaControllerTest extends PruebaRestDeReservas {

    private static final String ADMIN = SesionDePrueba.ADMINISTRADOR;

    private static String nota(String texto) {
        return "{\"texto\": \"" + texto + "\"}";
    }

    private ResultActions escribir(String codigo, String texto) throws Exception {
        return enviar(post("/api/reservas/" + codigo + "/notas"), nota(texto));
    }

    private String idDeLaUltima(String codigo, String texto) throws Exception {
        return JsonPath.read(escribir(codigo, texto).andReturn().getResponse().getContentAsString(), "$[0].id");
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void deberiaEscribirNotasYListarlasDeLaMasRecienteALaMasAntigua() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));

        escribir(codigo, "Pagaron la mitad, cobrar el resto")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservas/" + codigo + "/notas"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].autor").value(ADMIN))
                .andExpect(jsonPath("$[0].atendida").value(false))
                .andExpect(jsonPath("$[0].atencion").value(nullValue()));
        escribir(codigo, "Falta la cédula del acompañante").andExpect(status().isCreated());

        mvc.perform(get("/api/reservas/" + codigo + "/notas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].texto").value("Falta la cédula del acompañante"))
                .andExpect(jsonPath("$[1].texto").value("Pagaron la mitad, cobrar el resto"));
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void deberiaAtenderUnaNotaUnaSolaVez() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        String id = idDeLaUltima(codigo, "Pidieron dos cobijas adicionales");

        mvc.perform(put("/api/reservas/" + codigo + "/notas/" + id + "/atender"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atendida").value(true))
                .andExpect(jsonPath("$[0].atencion.autor").value(ADMIN))
                .andExpect(jsonPath("$[0].atencion.fechaHora").exists());
        mvc.perform(put("/api/reservas/" + codigo + "/notas/" + id + "/atender"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("YA_ATENDIDA"));
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-105")
    void enUnaReservaTerminadaLasNotasSeLeenPeroNoCambian() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        String id = idDeLaUltima(codigo, "Sale muy temprano");
        mvc.perform(put("/api/reservas/" + codigo + "/cancelar")).andExpect(status().isOk());

        escribir(codigo, "Llamó a reclamar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"));
        mvc.perform(put("/api/reservas/" + codigo + "/notas/" + id + "/atender"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"));
        mvc.perform(get("/api/reservas/" + codigo + "/notas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Tag("BL-08")
    void deberiaResponder400SinTextoY404SiNoExisteLaReservaOLaNotaEsDeOtra() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        String otra = reservaPendiente(HOY.plusDays(10));
        String deLaOtra = idDeLaUltima(otra, "Nota de otra reserva");

        escribir(codigo, " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        enviar(post("/api/reservas/" + codigo + "/notas"), nota("x".repeat(501)))
                .andExpect(status().isBadRequest());
        escribir("RES-2026-99999", "Sin reserva").andExpect(status().isNotFound());
        mvc.perform(get("/api/reservas/RES-2026-99999/notas")).andExpect(status().isNotFound());
        mvc.perform(put("/api/reservas/" + codigo + "/notas/" + deLaOtra + "/atender"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("BL-08")
    @Tag("DEC-67")
    void lasNotasSonDelPersonalYNoDelHuesped() throws Exception {
        String codigo = reservaPendiente(HOY.plusDays(5));
        String notas = "/api/reservas/" + codigo + "/notas";

        mvc.perform(post(notas).with(SesionDePrueba.como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON).content(nota("Falta la transferencia")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].autor").value(SesionDePrueba.idDe(Rol.RECEPCIONISTA)));
        mvc.perform(get(notas).with(SesionDePrueba.como(Rol.HUESPED))).andExpect(status().isForbidden());
        mvc.perform(get(notas).with(SesionDePrueba.como(Rol.PERSONAL_SERVICIO))).andExpect(status().isForbidden());
    }
}
