package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El envío del TRA y el RNT del alojamiento por REST (DEC-130). El flujo completo del envío está en
 * {@code EnviarTraTest}; aquí, el contrato, los rechazos y quién entra.
 */
@Tag("DEC-130")
class TraControllerTest extends PruebaRestDeReservas {

    @Test
    void elRntLoEscribeElAdministradorYSeVeEnElAlojamiento() throws Exception {
        enviar(put("/api/alojamiento/rnt"), "{\"rnt\": \"123456\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rnt").value("123456"));
        mvc.perform(get("/api/alojamiento")).andExpect(jsonPath("$.rnt").value("123456"));

        enviar(put("/api/alojamiento/rnt"), "{\"rnt\": \"12A\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("rnt: son solo números, hasta 20")));
        enviar(put("/api/alojamiento/rnt"), "{\"rnt\": \"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rnt").doesNotExist());
    }

    @Test
    void unaReservaSinLlegadaNoTieneEnvio() throws Exception {
        mvc.perform(get("/api/reservas/RES-2026-99999/tra")).andExpect(status().isNotFound());
        mvc.perform(put("/api/reservas/RES-2026-99999/tra/enviar")).andExpect(status().isNotFound());
    }

    @Test
    void laListaDeEnviosEsPorEstadoYDe10En10() throws Exception {
        mvc.perform(get("/api/tra/envios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty());
        mvc.perform(get("/api/tra/envios").param("estado", "OTRO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recepcionVeLosEnviosPeroNoCambiaElRntYServicioNoEntra() throws Exception {
        mvc.perform(get("/api/tra/envios").with(SesionDePrueba.como(Rol.RECEPCIONISTA))).andExpect(status().isOk());
        mvc.perform(put("/api/alojamiento/rnt").with(SesionDePrueba.como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rnt\": \"1\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tra/envios").with(SesionDePrueba.como(Rol.PERSONAL_SERVICIO)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/RES-2026-99999/tra").with(SesionDePrueba.como(Rol.PERSONAL_SERVICIO)))
                .andExpect(status().isForbidden());
    }
}
