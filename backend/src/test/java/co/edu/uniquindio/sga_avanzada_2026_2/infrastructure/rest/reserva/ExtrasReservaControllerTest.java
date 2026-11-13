package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PruebaRestDeReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cambiar solo los vehículos y las mascotas sin volver a cotizar las noches (R3 · DEC-116). El hotel cobra el
 * parqueadero por noche (carro 15.000, moto 8.000), tiene dos cupos y admite una mascota (30.000).
 */
class ExtrasReservaControllerTest extends PruebaRestDeReservas {

    @BeforeEach
    void prepararExtras() throws Exception {
        enviar(put("/api/alojamiento/parametros"), """
                {
                  "umbralEdadFacturable": 12, "horaEntrada": "15:00", "horaSalida": "11:00",
                  "tiempoPreparacionHoras": 3, "plazoConfirmacionHoras": 24, "horaLimiteNoShow": "22:00",
                  "anticipoPct": 30, "minimoMediosPago": 1, "minimoServiciosAdicionales": 1, "minimoTemporadas": 0,
                  "minimoTramosCancelacion": 1, "minimoCapacidadesDistintas": 1, "pagoEnLinea": "ANTICIPO",
                  "plazoPagoEnLineaMinutos": 30, "cupoParqueadero": 2, "aceptaMascotas": true, "limiteMascotas": 1,
                  "cargoMascota": 30000, "cobroParqueadero": "POR_NOCHE", "cargoCarro": 15000, "cargoMoto": 8000
                }
                """).andExpect(status().isOk());
    }

    /** Ana y Luis, dos noches en el APT-101 (400.000), con un carro (30.000) y una mascota (30.000). */
    private String reservaConCarroYMascota(LocalDate entrada) throws Exception {
        String json = reservaJson("APT-101", entrada, entrada.plusDays(2))
                .replaceFirst("\\{", "{\"carros\": 1, \"motos\": 0, \"mascotas\": 1,");
        String respuesta = enviar(post("/api/reservas"), json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor.total").value(400000))
                .andExpect(jsonPath("$.extras.total").value(60000))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.codigo");
    }

    private static String extras(String codigo) {
        return "/api/reservas/" + codigo + "/extras";
    }

    private static String cuerpo(int carros, int motos, int mascotas) {
        return "{\"carros\": %d, \"motos\": %d, \"mascotas\": %d}".formatted(carros, motos, mascotas);
    }

    @Test
    @Tag("DEC-116")
    @Tag("BL-23")
    void quitarLaMascotaNoVuelveACotizarLasNochesAunqueLaTarifaHayaSubido() throws Exception {
        String codigo = reservaConCarroYMascota(HOY.plusDays(5));
        enviar(put("/api/apartamentos/APT-101/tarifas/TEM-900"), "{\"valorPorOcupante\": 150000}")
                .andExpect(status().isOk());

        // Llegó sin la mascota y con una moto: las noches siguen en 400.000
        enviar(put(extras(codigo)), cuerpo(1, 1, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carros").value(1))
                .andExpect(jsonPath("$.motos").value(1))
                .andExpect(jsonPath("$.mascotas").value(0))
                .andExpect(jsonPath("$.valor.total").value(400000))
                .andExpect(jsonPath("$.extras.parqueadero").value(46000)) // (15.000 + 8.000) × 2 noches
                .andExpect(jsonPath("$.extras.mascotas").value(0))
                .andExpect(jsonPath("$.extras.total").value(46000));

        // El folio guarda solo la diferencia de los extras: 60.000 → 46.000
        mvc.perform(get("/api/reservas/" + codigo + "/folio"))
                .andExpect(jsonPath("$.cargos[-1].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.cargos[-1].concepto").value("Cambio de vehículos y mascotas"))
                .andExpect(jsonPath("$.cargos[-1].sentido").value("DISMINUYE"))
                .andExpect(jsonPath("$.cargos[-1].valor").value(14000))
                .andExpect(jsonPath("$.cargos", hasSize(4)));

        // Sin cambios, no hay ajuste nuevo
        enviar(put(extras(codigo)), cuerpo(1, 1, 0)).andExpect(status().isOk());
        mvc.perform(get("/api/reservas/" + codigo + "/folio")).andExpect(jsonPath("$.cargos", hasSize(4)));
    }

    @Test
    @Tag("DEC-116")
    @Tag("RP-02")
    @Tag("RP-03")
    void lasReglasDeCrearTambienAplicanYElCuerpoVaCompleto() throws Exception {
        String codigo = reservaConCarroYMascota(HOY.plusDays(5));

        enviar(put(extras(codigo)), cuerpo(1, 0, 2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("MASCOTAS_NO_ADMITIDAS"));
        enviar(put(extras(codigo)), cuerpo(2, 1, 1)).andExpect(status().isConflict());
        enviar(put(extras(codigo)), cuerpo(1, -1, 1)).andExpect(status().isBadRequest());
        enviar(put(extras(codigo)), "{\"carros\": 1, \"mascotas\": 1}").andExpect(status().isBadRequest());
        enviar(put(extras("RES-2026-99999")), cuerpo(0, 0, 0)).andExpect(status().isNotFound());

        // Nada de lo rechazado quedó guardado
        mvc.perform(get("/api/reservas/" + codigo))
                .andExpect(jsonPath("$.carros").value(1))
                .andExpect(jsonPath("$.mascotas").value(1))
                .andExpect(jsonPath("$.extras.total").value(60000));
    }

    @Test
    @Tag("DEC-116")
    void enCursoNoSeCambianLosExtrasSeCarganComoConsumo() throws Exception {
        String codigo = reservaEnCurso();

        enviar(put(extras(codigo)), cuerpo(1, 0, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("ESTADO_NO_PERMITIDO"));
    }

    @Test
    @Tag("DEC-116")
    @Tag("DEC-99")
    void soloRecepcionYAdministracionCambianLosExtras() throws Exception {
        String codigo = reservaConCarroYMascota(HOY.plusDays(5));
        String titular = JsonPath.read(mvc.perform(get("/api/reservas/" + codigo))
                .andReturn().getResponse().getContentAsString(), "$.titularId");

        mvc.perform(put(extras(codigo)).with(SesionDePrueba.como(Rol.HUESPED, titular))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, 0, 0)))
                .andExpect(status().isForbidden());
        mvc.perform(put(extras(codigo)).with(SesionDePrueba.como(Rol.PERSONAL_SERVICIO))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, 0, 0)))
                .andExpect(status().isForbidden());
        mvc.perform(put(extras(codigo)).with(SesionDePrueba.como(Rol.RECEPCIONISTA))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(1, 0, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mascotas").value(0));
    }
}
