package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

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

/** El interruptor del IVA de la estancia por REST (DEC-133). */
@Tag("DEC-133")
class ImpuestoAlojamientoControllerTest extends PruebaRestDeReservas {

    @Test
    void vieneApagadoConEl19YElAdministradorLoEnciende() throws Exception {
        mvc.perform(get("/api/alojamiento"))
                .andExpect(jsonPath("$.impuesto.cobraIva").value(false))
                .andExpect(jsonPath("$.impuesto.porcentaje").value(19));

        enviar(put("/api/alojamiento/iva"), "{\"cobraIva\": true, \"porcentaje\": 19}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impuesto.cobraIva").value(true));
        mvc.perform(get("/api/alojamiento")).andExpect(jsonPath("$.impuesto.cobraIva").value(true));
    }

    @Test
    void unPorcentajeFueraDeRangoEs400YCobrarConCeroEs409() throws Exception {
        enviar(put("/api/alojamiento/iva"), "{\"cobraIva\": true, \"porcentaje\": 120}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("porcentaje: va de 0 a 100")));
        enviar(put("/api/alojamiento/iva"), "{\"porcentaje\": 19}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").value(hasItem("cobraIva: es obligatorio")));
        enviar(put("/api/alojamiento/iva"), "{\"cobraIva\": true, \"porcentaje\": 0}")
                .andExpect(status().isConflict());
    }

    @Test
    void recepcionNoLoCambia() throws Exception {
        mvc.perform(put("/api/alojamiento/iva").with(SesionDePrueba.como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cobraIva\": true, \"porcentaje\": 19}"))
                .andExpect(status().isForbidden());
    }
}
