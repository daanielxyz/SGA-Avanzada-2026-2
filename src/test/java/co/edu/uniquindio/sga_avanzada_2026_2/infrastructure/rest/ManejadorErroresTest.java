package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errores que ningún endpoint provoca a voluntad en una prueba: se montan sobre un controlador de mentira, solo con
 * el manejador global (DEC-33 · DEC-64).
 */
class ManejadorErroresTest {

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/concurrencia")
        String chocar() {
            throw new OptimisticLockingFailureException("otra transacción guardó primero");
        }

        @GetMapping("/prueba/parametro")
        String exigir(@RequestParam String desde) {
            return desde;
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new ManejadorErrores())
            .build();

    @Test
    @Tag("DEC-17")
    void unChoqueDeBloqueoOptimistaEs409ConSuCodigoPropio() throws Exception {
        mvc.perform(get("/prueba/concurrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.codigo").value("CONCURRENCIA"))
                .andExpect(jsonPath("$.detalles").isEmpty());
    }

    @Test
    void unParametroObligatorioQueFaltaEs400() throws Exception {
        mvc.perform(get("/prueba/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("desde: es obligatorio")));
    }
}
