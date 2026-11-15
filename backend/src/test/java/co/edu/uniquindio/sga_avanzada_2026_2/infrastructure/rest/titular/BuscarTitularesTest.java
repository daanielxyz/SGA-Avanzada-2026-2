package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba.como;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El buscador de titulares por REST (DEC-131): por nombre o documento, la base filtra y pagina.
 */
class BuscarTitularesTest extends PruebaRestDeReservas {

    @BeforeEach
    void registrarTitulares() throws Exception {
        registrar("Zoe", "Ríos Pérez", "CC", "1094000");
        registrar("Ana María", "Ríos", "CC", "52481");
        registrar("Andrés", "Gómez", "PASAPORTE", "AB123456");
    }

    private void registrar(String nombres, String apellidos, String tipo, String numero) throws Exception {
        String[] partes = apellidos.split(" ");
        String segundo = partes.length > 1 ? ", \"segundoApellido\": \"" + partes[1] + "\"" : "";
        enviar(post("/api/titulares"), """
                {"nombres": "%s", "primerApellido": "%s"%s, "tipoDocumento": "%s", "numeroDocumento": "%s",
                 "correo": "%s@correo.co"}
                """.formatted(nombres, partes[0], segundo, tipo, numero, numero.toLowerCase()))
                .andExpect(status().isCreated());
    }

    @Test
    @Tag("DEC-131")
    void deberiaEncontrarPorPalabrasDelNombreSinTildesYOrdenarPorNombre() throws Exception {
        mvc.perform(get("/api/titulares").param("buscar", "RIOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].nombre").value(contains("Ana María Ríos", "Zoe Ríos Pérez")))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/titulares").param("buscar", "perez zoe"))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numeroDocumento").value("1094000"));
    }

    @Test
    @Tag("DEC-131")
    void deberiaEncontrarPorElDocumentoConPuntosOEnMinusculas() throws Exception {
        mvc.perform(get("/api/titulares").param("buscar", "1.094.000"))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Zoe Ríos Pérez"));
        mvc.perform(get("/api/titulares").param("buscar", "ab123"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Andrés Gómez"));
        mvc.perform(get("/api/titulares").param("buscar", "nadie"))
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @Tag("DEC-131")
    void sinTextoTraeATodosYLosComodinesSeTomanComoLetras() throws Exception {
        mvc.perform(get("/api/titulares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Ana María Ríos"))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get("/api/titulares").param("buscar", "%"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @Tag("DEC-131")
    void unTextoDeMasDeCienCaracteresOUnaPaginaNegativaSon400() throws Exception {
        mvc.perform(get("/api/titulares").param("buscar", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        mvc.perform(get("/api/titulares").param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Tag("DEC-131")
    void recepcionBuscaYElPersonalDeServicioNo() throws Exception {
        mvc.perform(get("/api/titulares").param("buscar", "ana").with(como(Rol.RECEPCIONISTA)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/titulares").with(como(Rol.PERSONAL_SERVICIO)))
                .andExpect(status().isForbidden());
    }
}
