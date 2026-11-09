package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas REST de la operación (B2b y B2c), de extremo a extremo con H2: el alojamiento de
 * {@link AlojamientoDePrueba} con el APT-101 (capacidad 4) y el APT-102 (capacidad 2), activos, PREPARADOS y a
 * 100.000 por ocupante y noche. El reloj es el real: las fechas son relativas a hoy en Colombia. Cada paso previo se
 * da por su endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class PruebaRestDeReservas {

    protected static final LocalDate HOY = AlojamientoDePrueba.hoy();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private AlojamientoRepository alojamientos;

    @Autowired
    private CalendarioTemporadasRepository calendarios;

    @Autowired
    private PoliticaCancelacionRepository politicas;

    @Autowired
    private ApartamentoRepository apartamentos;

    @Autowired
    private TarifaRepository tarifas;

    @BeforeEach
    void prepararAlojamiento() {
        AlojamientoDePrueba.guardar(alojamientos, calendarios, politicas);
        AlojamientoDePrueba.guardarApartamento(apartamentos, tarifas, "APT-101", 4);
        AlojamientoDePrueba.guardarApartamento(apartamentos, tarifas, "APT-102", 2);
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Ana (titular, CC 1094) y Luis, dos adultos, en el apartamento y las fechas dadas. */
    protected static String reservaJson(String apartamento, LocalDate entrada, LocalDate salida) {
        return """
                {
                  "apartamento": "%s",
                  "entrada": "%s",
                  "salida": "%s",
                  "canalOrigen": "DIRECTO",
                  "titular": {"nombre": "Ana Ríos", "tipoDocumento": "CC", "numeroDocumento": "1094",
                              "correo": "ana@correo.co", "telefono": "300 123 4567"},
                  "ocupantes": [
                    {"nombre": "Ana Ríos", "fechaNacimiento": "1990-05-01", "tipoDocumento": "CC",
                     "numeroDocumento": "1094", "nacionalidad": "Colombiana"},
                    {"nombre": "Luis Ríos", "fechaNacimiento": "1988-03-02"}
                  ]
                }
                """.formatted(apartamento, entrada, salida);
    }

    /** Reserva PENDIENTE de dos adultos por dos noches en el APT-101: 400.000. Devuelve su código. */
    protected String reservaPendiente(LocalDate entrada) throws Exception {
        String respuesta = enviar(post("/api/reservas"), reservaJson("APT-101", entrada, entrada.plusDays(2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.codigo");
    }

    /** Reserva CONFIRMADA: indicó su hora de llegada y pagó el anticipo del 30 % (120.000) en efectivo. */
    protected String reservaConfirmada(LocalDate entrada) throws Exception {
        String codigo = reservaPendiente(entrada);
        enviar(put("/api/reservas/" + codigo + "/hora-llegada"), "{\"hora\": \"16:00\"}").andExpect(status().isOk());
        pagar(codigo, 120_000);
        mvc.perform(put("/api/reservas/" + codigo + "/confirmar")).andExpect(status().isOk());
        return codigo;
    }

    /** Cuerpo para registrar una llegada sin haber pagado toda la estancia (BL-22). */
    protected static final String ENTRA_SIN_PAGAR = "{\"motivoSinPago\": \"Cuenta por cobrar\"}";

    /** Reserva EN_CURSO: entra hoy y ya se registró su llegada. */
    protected String reservaEnCurso() throws Exception {
        String codigo = reservaConfirmada(HOY);
        enviar(put("/api/reservas/" + codigo + "/registrar-llegada"), ENTRA_SIN_PAGAR).andExpect(status().isOk());
        completarDatosTra(codigo, "OCU-1", datosTra("Ana", "FEMENINO", null)).andExpect(status().isOk());
        completarDatosTra(codigo, "OCU-2", datosTra("Luis", "MASCULINO", "1095")).andExpect(status().isOk());
        return codigo;
    }

    /** Lo que el TRA pide de un ocupante (BL-07); el documento, solo si no llegó al reservar. */
    protected static String datosTra(String nombres, String genero, String cedula) {
        String documento = cedula == null ? ""
                : "\"tipoDocumento\": \"CC\", \"numeroDocumento\": \"%s\", ".formatted(cedula);
        return """
                {%s"nombres": "%s", "primerApellido": "Ríos", "genero": "%s", "nacionalidad": "Colombiana",
                 "paisResidencia": "Colombia", "ciudadResidencia": "Pereira", "ciudadProcedencia": "Pereira",
                 "ciudadDestino": "Salento", "motivoViaje": "TURISMO_VACACIONES"}"""
                .formatted(documento, nombres, genero);
    }

    protected ResultActions completarDatosTra(String codigo, String ocupante, String json) throws Exception {
        return enviar(put("/api/reservas/" + codigo + "/ocupantes/" + ocupante + "/datos-tra"), json);
    }

    protected void pagar(String codigo, long monto) throws Exception {
        enviar(post("/api/reservas/" + codigo + "/folio/pagos"), pagoJson("EFECTIVO", monto, HOY))
                .andExpect(status().isCreated());
    }

    protected static String pagoJson(String medio, long monto, LocalDate fecha) {
        return "{\"medio\": \"%s\", \"monto\": %d, \"fecha\": \"%s\"}".formatted(medio, monto, fecha);
    }
}
