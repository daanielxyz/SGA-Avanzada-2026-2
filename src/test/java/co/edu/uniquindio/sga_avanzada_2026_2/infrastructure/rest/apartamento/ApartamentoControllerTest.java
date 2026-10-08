package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Corte de extremo a extremo (A1-E): HTTP → Request → Command → caso de uso → dominio → JPA (H2) → Result →
 * Response, y los errores uniformes 400/404/409 (DEC-31..33).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApartamentoControllerTest {

    private static final String VALIDO = """
            {
              "codigo": "APT-101",
              "alojamientoId": "ALO-1",
              "nombre": "Apartamento 101",
              "descripcion": "Vista al mar",
              "capacidad": 4,
              "dormitorios": 2,
              "imagenes": [{"url": "https://cdn.sga.co/apt-101.jpg", "principal": true}],
              "caracteristicas": ["Balcón"]
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlojamientoRepository alojamientos;

    @BeforeEach
    void crearAlojamiento() {
        alojamientos.guardar(new Alojamiento(new AlojamientoId("ALO-1"), "Puerta al Sol", "Frente al mar",
                "Santa Marta", "Calle 1 # 2-3", new Ubicacion(11.24, -74.21), null,
                new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                        Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2, 2, 2),
                List.of(new ServicioAdicional(new ServicioAdicionalId("SRV-1"), "Desayuno", true, Dinero.de(25_000), true)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA"))));
    }

    private void crear(String json) throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void deberiaCrearYResponder201ConUbicacionYCuerpo() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/apartamentos/APT-101"))
                .andExpect(jsonPath("$.codigo").value("APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.imagenes[0].principal").value(true))
                .andExpect(jsonPath("$.caracteristicas[0]").value("Balcón"));
    }

    @Test
    void deberiaConsultarLoQueSeCreo() throws Exception {
        crear(VALIDO);

        mvc.perform(get("/api/apartamentos/APT-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Apartamento 101"))
                .andExpect(jsonPath("$.capacidad").value(4))
                .andExpect(jsonPath("$.bloqueos").isEmpty());
    }

    @Test
    void deberiaResponder400ConLosCamposInvalidos() throws Exception {
        String invalido = VALIDO.replace("\"APT-101\"", "\"101\"").replace("\"capacidad\": 4", "\"capacidad\": 0")
                .replace("\"nombre\": \"Apartamento 101\",", "");

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").value(hasItem("capacidad: debe ser al menos 1")))
                .andExpect(jsonPath("$.detalles").value(hasItem("codigo: debe tener el formato APT-101")))
                .andExpect(jsonPath("$.detalles").value(hasItem("nombre: es obligatorio")));
    }

    @Test
    void deberiaResponder400SiElJsonEstaMalFormado() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content("{ \"codigo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void deberiaResponder404SiElApartamentoNoExiste() throws Exception {
        mvc.perform(get("/api/apartamentos/APT-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void deberiaResponder404SiElAlojamientoNoExiste() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("ALO-1", "ALO-9")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void deberiaResponder409SiElCodigoYaExiste() throws Exception {
        crear(VALIDO);

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"));
    }

    @Test
    void deberiaResponder409SiElFormatoEsValidoPeroViolaUnaReglaDeNegocio() throws Exception {
        String dosPrincipales = VALIDO.replace(
                "[{\"url\": \"https://cdn.sga.co/apt-101.jpg\", \"principal\": true}]",
                "[{\"url\": \"https://cdn.sga.co/a.jpg\", \"principal\": true},"
                        + " {\"url\": \"https://cdn.sga.co/b.jpg\", \"principal\": true}]");

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(dosPrincipales))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value("Exactamente una imagen debe ser la principal"));
    }
}
