package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Corregir el documento mal escrito de un ocupante o del titular sin perder sus datos del TRA (#14 · BL-07 · DEC-120).
 * Ana (titular, CC 1094) y Luis (CC 1095 desde sus datos del TRA) en una reserva en curso.
 */
class CorregirDocumentoTest extends PruebaRestDeReservas {

    private static String documento(String codigo, String ocupante) {
        return "/api/reservas/" + codigo + "/ocupantes/" + ocupante + "/documento";
    }

    private static String cuerpo(String tipo, String numero, String motivo) {
        return "{\"tipoDocumento\": \"%s\", \"numeroDocumento\": \"%s\", \"motivo\": \"%s\"}"
                .formatted(tipo, numero, motivo);
    }

    @Test
    @Tag("DEC-120")
    @Tag("BL-07")
    void corregirElDocumentoConservaLosDatosDelTraYQuedaRegistrado() throws Exception {
        String codigo = reservaEnCurso();

        enviar(put(documento(codigo, "OCU-2")), cuerpo("CC", "1096", "Se digitó mal el último número"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes[1].numeroDocumento").value("1096"))
                .andExpect(jsonPath("$.ocupantes[1].tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.ocupantes[1].ciudadProcedencia").value("Pereira"))
                .andExpect(jsonPath("$.ocupantes[1].motivoViaje").value("TURISMO_VACACIONES"))
                .andExpect(jsonPath("$.ocupantes[1].datosTraFaltantes", hasSize(0)))
                .andExpect(jsonPath("$.correccionesDocumento", hasSize(1)))
                .andExpect(jsonPath("$.correccionesDocumento[0].ocupante").value("Luis Ríos"))
                .andExpect(jsonPath("$.correccionesDocumento[0].numeroAnterior").value("1095"))
                .andExpect(jsonPath("$.correccionesDocumento[0].numeroNuevo").value("1096"))
                .andExpect(jsonPath("$.correccionesDocumento[0].motivo").value("Se digitó mal el último número"))
                .andExpect(jsonPath("$.correccionesDocumento[0].autor").value(SesionDePrueba.ADMINISTRADOR))
                .andExpect(jsonPath("$.correccionesDocumento[0].fechaHora").isNotEmpty());

        // Otro tipo de documento también se corrige, y el historial guarda las dos
        enviar(put(documento(codigo, "OCU-2")), cuerpo("CE", "1096", "Es cédula de extranjería"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/reservas/" + codigo))
                .andExpect(jsonPath("$.ocupantes[1].tipoDocumento").value("CE"))
                .andExpect(jsonPath("$.correccionesDocumento", hasSize(2)))
                .andExpect(jsonPath("$.correccionesDocumento[1].tipoAnterior").value("CC"));
    }

    @Test
    @Tag("DEC-120")
    @Tag("OCU-05")
    void lasReglasDeLaCorreccion() throws Exception {
        String codigo = reservaEnCurso();

        enviar(put(documento(codigo, "OCU-2")), cuerpo("CC", "1095", "Igual"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
        enviar(put(documento(codigo, "OCU-2")), cuerpo("CC", "1094", "Es el de Ana"))
                .andExpect(status().isConflict());
        enviar(put(documento(codigo, "OCU-2")), cuerpo("CC", "1096", " ")).andExpect(status().isBadRequest());
        enviar(put(documento(codigo, "OCU-2")), cuerpo("DNI", "1096", "Otro tipo")).andExpect(status().isBadRequest());
        enviar(put(documento(codigo, "OCU-9")), cuerpo("CC", "1096", "No existe")).andExpect(status().isConflict());

        // Luis no tiene documento en una reserva recién creada: se agrega con sus datos del TRA
        String pendiente = reservaPendiente(HOY.plusDays(20));
        enviar(put(documento(pendiente, "OCU-2")), cuerpo("CC", "1096", "Sin documento"))
                .andExpect(status().isConflict());

        // Una reserva cancelada ya no se corrige
        mvc.perform(put("/api/reservas/" + pendiente + "/cancelar")).andExpect(status().isOk());
        enviar(put(documento(pendiente, "OCU-1")), cuerpo("CC", "1097", "Tarde"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"));

        // Nada de lo rechazado quedó en el historial
        mvc.perform(get("/api/reservas/" + codigo)).andExpect(jsonPath("$.correccionesDocumento", hasSize(0)));
    }

    @Test
    @Tag("DEC-120")
    @Tag("TIT-05")
    void corregirAlTitularQueSeAlojaCorrigeTambienAlTitular() throws Exception {
        String codigo = reservaEnCurso();
        String titular = titularDe(codigo);

        enviar(put(documento(codigo, "OCU-1")), cuerpo("CC", "10940", "Faltaba un cero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titularId").value(titular))
                .andExpect(jsonPath("$.ocupantes[0].numeroDocumento").value("10940"))
                .andExpect(jsonPath("$.ocupantes[0].ciudadProcedencia").value("Pereira"))
                .andExpect(jsonPath("$.correccionesDocumento[0].ocupante").value("Ana Ríos"))
                .andExpect(jsonPath("$.correccionesDocumento[0].numeroAnterior").value("1094"))
                .andExpect(jsonPath("$.correccionesDocumento[0].titular").value(true));
        mvc.perform(get("/api/titulares/" + titular))
                .andExpect(jsonPath("$.numeroDocumento").value("10940"));

        // Quien vuelve a reservar con el documento corregido es el mismo titular (TIT-05)
        String otra = enviar(post("/api/reservas"),
                reservaJson("APT-102", HOY.plusDays(30), HOY.plusDays(32)).replace("1094", "10940"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertEquals(titular, JsonPath.read(otra, "$.titularId"));

        // Un ocupante que no es el titular no lo toca
        enviar(put(documento(codigo, "OCU-2")), cuerpo("CC", "1096", "Mal digitado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correccionesDocumento[1].titular").value(false));
        mvc.perform(get("/api/titulares/" + titular)).andExpect(jsonPath("$.numeroDocumento").value("10940"));
    }

    @Test
    @Tag("DEC-120")
    @Tag("TIT-01")
    void elTitularQueNoSeAlojaSeCorrigePorSuRuta() throws Exception {
        String codigo = reservaEnCurso();
        String marta = """
                {"nombre": "Marta Ríos", "tipoDocumento": "CC", "numeroDocumento": "3030",
                 "correo": "marta@correo.co", "telefono": "300 765 4321"}""";
        String titular = JsonPath.read(enviar(put("/api/reservas/" + codigo + "/cambiar-titular"), marta)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.titularId");
        String ruta = "/api/reservas/" + codigo + "/titular/documento";

        enviar(put(ruta), cuerpo("CE", "3031", "Es cédula de extranjería"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titularId").value(titular))
                .andExpect(jsonPath("$.ocupantes[0].numeroDocumento").value("1094"))
                .andExpect(jsonPath("$.correccionesDocumento", hasSize(1)))
                .andExpect(jsonPath("$.correccionesDocumento[0].ocupante").value("Marta Ríos"))
                .andExpect(jsonPath("$.correccionesDocumento[0].numeroAnterior").value("3030"))
                .andExpect(jsonPath("$.correccionesDocumento[0].tipoNuevo").value("CE"))
                .andExpect(jsonPath("$.correccionesDocumento[0].titular").value(true));
        mvc.perform(get("/api/titulares/" + titular))
                .andExpect(jsonPath("$.tipoDocumento").value("CE"))
                .andExpect(jsonPath("$.numeroDocumento").value("3031"));

        enviar(put(ruta), cuerpo("CE", "3031", "Igual")).andExpect(status().isConflict());
        enviar(put(ruta), cuerpo("RC", "3032", "Registro civil"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("TITULAR_INVALIDO"));
        // El documento de Ana ya es de otro titular del alojamiento (TIT-05)
        enviar(put(ruta), cuerpo("CC", "1094", "Es el de Ana"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("otro titular")));
        enviar(put(ruta), cuerpo("CE", "3032", " ")).andExpect(status().isBadRequest());
        mvc.perform(put(ruta).with(SesionDePrueba.como(Rol.HUESPED, titular))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("CE", "3032", "Yo")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/reservas/" + codigo)).andExpect(jsonPath("$.correccionesDocumento", hasSize(1)));
        mvc.perform(get("/api/titulares/" + titular)).andExpect(jsonPath("$.numeroDocumento").value("3031"));
    }

    private String titularDe(String codigo) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/reservas/" + codigo))
                .andReturn().getResponse().getContentAsString(), "$.titularId");
    }

    @Test
    @Tag("DEC-120")
    @Tag("DEC-103")
    void soloElPersonalDeRecepcionCorrigeYElHuespedNoVeElHistorial() throws Exception {
        String codigo = reservaEnCurso();
        String titular = JsonPath.read(mvc.perform(get("/api/reservas/" + codigo))
                .andReturn().getResponse().getContentAsString(), "$.titularId");

        mvc.perform(put(documento(codigo, "OCU-2")).with(SesionDePrueba.como(Rol.HUESPED, titular))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("CC", "1096", "Yo")))
                .andExpect(status().isForbidden());
        mvc.perform(put(documento(codigo, "OCU-2")).with(SesionDePrueba.como(Rol.PERSONAL_SERVICIO))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("CC", "1096", "Yo")))
                .andExpect(status().isForbidden());
        mvc.perform(put(documento(codigo, "OCU-2")).with(SesionDePrueba.como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("CC", "1096", "Mal digitado")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reservas/" + codigo).with(SesionDePrueba.como(Rol.HUESPED, titular)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupantes[1].numeroDocumento").value("1096"))
                .andExpect(jsonPath("$.correccionesDocumento").value(nullValue()));
    }
}
