package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.idDe;
import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.sinSesion;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qué deja hacer cada rol (B3a · DEC-22 · DEC-67), sobre los apartamentos de {@link PruebaRestDeReservas}. Los
 * pasos previos de cada prueba los da el administrador de {@link SesionDePrueba}.
 */
class PermisosPorRolTest extends PruebaRestDeReservas {

    private static final String GRIFO = "{\"descripcion\": \"Grifo del baño gotea\", \"gravedad\": \"MEDIA\"}";
    private static final String CON_MOTIVO = "{\"motivoAutorizacion\": \"Convenio con la empresa\"}";

    private ResultActions como(Rol rol, MockHttpServletRequestBuilder peticion) throws Exception {
        return mvc.perform(peticion.with(SesionDePrueba.como(rol)));
    }

    private ResultActions como(Rol rol, MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return como(rol, peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String estado(String estado) {
        return "{\"estado\": \"" + estado + "\"}";
    }

    @Test
    @Tag("DEC-67")
    void sinSesionSoloSeEntraALoPublico() throws Exception {
        mvc.perform(get("/api/apartamentos/APT-101").with(sinSesion())).andExpect(status().isOk());
        mvc.perform(get("/api/reservas/RES-2026-00001").with(sinSesion()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(post("/api/apartamentos").with(sinSesion()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/apartamentos/APT-101/tarifas").with(sinSesion()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Tag("CAN-03")
    @Tag("DEC-73")
    void elContratoDelCanalPideLaClaveDelCanalYNoUnUsuario() throws Exception {
        mvc.perform(post("/api/canales-externos/CAN-99999/reservas").with(sinSesion())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/canales-externos/CAN-99999/disponibilidad").with(sinSesion())
                        .param("entrada", HOY.plusDays(5).toString()).param("salida", HOY.plusDays(6).toString())
                        .param("ocupantes", "2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CANAL_NO_AUTENTICADO"));
    }

    @Test
    @Tag("ROL-04")
    void elHuespedCotizaPeroNoOperaElAlojamiento() throws Exception {
        String cotizacion = ("{\"apartamento\": \"APT-101\", \"entrada\": \"%s\", \"salida\": \"%s\","
                + " \"fechasNacimiento\": [\"1990-05-01\"]}").formatted(HOY.plusDays(5), HOY.plusDays(7));

        como(Rol.HUESPED, post("/api/cotizaciones"), cotizacion).andExpect(status().isOk());
        como(Rol.HUESPED, post("/api/reservas"), reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        como(Rol.HUESPED, get("/api/canales")).andExpect(status().isForbidden());
        como(Rol.HUESPED, post("/api/apartamentos/APT-101/novedades"), GRIFO).andExpect(status().isForbidden());
    }

    @Test
    @Tag("ROL-04")
    @Tag("BLO-04")
    void elRecepcionistaVendeYCobraPeroNoConfigura() throws Exception {
        como(Rol.RECEPCIONISTA, post("/api/reservas"), reservaJson("APT-101", HOY.plusDays(5), HOY.plusDays(7)))
                .andExpect(status().isCreated());
        como(Rol.RECEPCIONISTA, get("/api/titulares/TIT-99999")).andExpect(status().isNotFound());

        como(Rol.RECEPCIONISTA, post("/api/apartamentos/APT-101/bloqueos"), "{}")
                .andExpect(status().isForbidden());
        como(Rol.RECEPCIONISTA, put("/api/alojamiento/parametros"), "{}").andExpect(status().isForbidden());
        como(Rol.RECEPCIONISTA, get("/api/apartamentos/APT-101/tarifas")).andExpect(status().isForbidden());
        como(Rol.RECEPCIONISTA, get("/api/politicas/vigente")).andExpect(status().isForbidden());
        como(Rol.RECEPCIONISTA, get("/api/conflictos/pendientes")).andExpect(status().isForbidden());
        como(Rol.RECEPCIONISTA, post("/api/apartamentos/APT-101/novedades"), GRIFO)
                .andExpect(status().isForbidden());
    }

    @Test
    @Tag("REG-06")
    void soloElAdministradorCorrigeUnRegistro() throws Exception {
        String codigo = reservaEnCurso();
        String correccion = "{\"fechaHora\": \"%s\", \"motivo\": \"Se registró al grupo equivocado\"}"
                .formatted(LocalDateTime.now(ZoneId.of("America/Bogota")).truncatedTo(ChronoUnit.SECONDS));

        como(Rol.RECEPCIONISTA, put("/api/reservas/" + codigo + "/corregir-registro"), correccion)
                .andExpect(status().isForbidden());
        enviar(put("/api/reservas/" + codigo + "/corregir-registro"), correccion).andExpect(status().isOk());
    }

    @Test
    @Tag("AUTC-01")
    @Tag("RN-17")
    void soloElAdministradorAutorizaCerrarUnFolioConSaldo() throws Exception {
        String cerrar = "/api/reservas/" + reservaEnCurso() + "/folio/cerrar";

        como(Rol.RECEPCIONISTA, put(cerrar), CON_MOTIVO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"))
                .andExpect(jsonPath("$.mensaje")
                        .value("Solo el administrador puede autorizar el cierre de un folio con saldo"));
        como(Rol.RECEPCIONISTA, put(cerrar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put(cerrar), CON_MOTIVO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizacion.autor").value(SesionDePrueba.ADMINISTRADOR));
    }

    @Test
    @Tag("AUTC-01")
    @Tag("SAL-02")
    void soloElAdministradorAutorizaUnaSalidaConSaldo() throws Exception {
        String codigo = reservaEnCurso();
        String salida = "/api/reservas/" + codigo + "/registrar-salida";

        como(Rol.RECEPCIONISTA, put(salida), CON_MOTIVO).andExpect(status().isForbidden());
        pagar(codigo, 280_000);
        como(Rol.RECEPCIONISTA, put(salida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.salida.autor").value(idDe(Rol.RECEPCIONISTA)));
    }

    @Test
    @Tag("EOPE-04")
    void elPersonalDeServicioSoloMueveLaRutaDePreparacion() throws Exception {
        String estadoOperativo = "/api/apartamentos/APT-101/estado-operativo";

        como(Rol.PERSONAL_SERVICIO, put(estadoOperativo), estado("FUERA_DE_SERVICIO"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        enviar(put(estadoOperativo), estado("FUERA_DE_SERVICIO")).andExpect(status().isOk());
        como(Rol.PERSONAL_SERVICIO, put(estadoOperativo), estado("PENDIENTE_PREPARACION"))
                .andExpect(status().isForbidden());
        enviar(put(estadoOperativo), estado("PENDIENTE_PREPARACION")).andExpect(status().isOk());

        como(Rol.PERSONAL_SERVICIO, put(estadoOperativo), estado("EN_PREPARACION")).andExpect(status().isOk());
        como(Rol.PERSONAL_SERVICIO, put(estadoOperativo), estado("PREPARADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
        como(Rol.RECEPCIONISTA, put(estadoOperativo), estado("EN_PREPARACION")).andExpect(status().isForbidden());
    }

    @Test
    @Tag("NOV-05")
    void elPersonalDeServicioReportaNovedadesPeroNoVende() throws Exception {
        como(Rol.PERSONAL_SERVICIO, post("/api/apartamentos/APT-101/novedades"), GRIFO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.historial[0].autor").value(idDe(Rol.PERSONAL_SERVICIO)));
        como(Rol.PERSONAL_SERVICIO, get("/api/apartamentos/APT-101/novedades")).andExpect(status().isOk());

        como(Rol.PERSONAL_SERVICIO, get("/api/reservas/RES-2026-00001")).andExpect(status().isForbidden());
        como(Rol.PERSONAL_SERVICIO, put("/api/apartamentos/APT-101/retirar")).andExpect(status().isForbidden());
    }

    @Test
    @Tag("DEC-67")
    void loQueNoEsDeLaApiNoSeAtiendeNiAlAdministrador() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
        mvc.perform(get("/api/no-existe").with(SesionDePrueba.como(Rol.RECEPCIONISTA)))
                .andExpect(status().isForbidden());
    }
}
